package com.groceryshop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.groceryshop.dto.OrderDTO;
import com.groceryshop.dto.OrderJob;
import com.groceryshop.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class OrderQueueService {

    private static final Logger log = LoggerFactory.getLogger(OrderQueueService.class);
    private static final String JOBS_FILE_PATH = "backups/.queue-jobs.json";

    @Autowired
    private OrderService orderService;

    @Autowired
    private SystemActivityService systemActivityService;

    private final AtomicBoolean workerPaused = new AtomicBoolean(false);
    private final AtomicLong jobSeq = new AtomicLong(1000);
    private final Object fileLock = new Object();
    private final ObjectMapper objectMapper;

    private final Map<String, OrderJob> jobsById = new ConcurrentHashMap<>();
    private final Map<Long, String> jobIdByOrderId = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<OrderDTO>> pendingFutures = new ConcurrentHashMap<>();

    /**
     * Hàng đợi ưu tiên với bộ phân xử tranh chấp 3 cấp độ:
     * - Cấp 1 (FIFO): Chênh lệch thời gian đặt > 10ms -> ai gửi trước xử lý trước
     * - Cấp 2 (Hạng thành viên): Cùng đợt 10ms -> DIAMOND > GOLD > SILVER > BRONZE
     * - Cấp 3 (Fair Lottery): Cùng đợt & cùng hạng -> bốc thăm ngẫu nhiên công bằng
     */
    private final PriorityBlockingQueue<OrderJob> queue = new PriorityBlockingQueue<>(64, (a, b) -> {
        long diff = a.getEnqueuedAtMillis() - b.getEnqueuedAtMillis();
        if (Math.abs(diff) > 10) {
            return Long.compare(a.getEnqueuedAtMillis(), b.getEnqueuedAtMillis());
        }
        int tierCompare = Integer.compare(tierRank(b.getMembershipTier()), tierRank(a.getMembershipTier()));
        if (tierCompare != 0) {
            return tierCompare;
        }
        return Integer.compare(a.getLotteryScore(), b.getLotteryScore());
    });

    public OrderQueueService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @PostConstruct
    public void init() {
        loadJobsFromFile();
    }

    private int tierRank(String tier) {
        if (tier == null) return 1;
        return switch (tier.toUpperCase()) {
            case "DIAMOND" -> 4;
            case "GOLD" -> 3;
            case "SILVER" -> 2;
            default -> 1; // BRONZE / STANDARD
        };
    }

    private String nowIso() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public boolean isPaused() {
        return workerPaused.get();
    }

    public OrderJob enqueueOrder(OrderDTO orderDto, String membershipTier) {
        long seq = jobSeq.incrementAndGet();
        String jobId = "JOB-" + seq;
        String now = nowIso();

        OrderJob job = OrderJob.builder()
                .jobId(jobId)
                .orderId(orderDto.getId())
                .userId(orderDto.getUserId())
                .customerName(orderDto.getShippingName())
                .membershipTier(membershipTier != null ? membershipTier : "BRONZE")
                .paymentMethod(orderDto.getPaymentMethod())
                .amount(orderDto.getFinalAmount() != null ? orderDto.getFinalAmount() : BigDecimal.ZERO)
                .status("PENDING")
                .progress(0)
                .retryCount(0)
                .deadLetter(false)
                .simulatedDlq(false)
                .resolutionTier("FIFO")
                .enqueuedAtMillis(System.currentTimeMillis())
                .lotteryScore(ThreadLocalRandom.current().nextInt(1, 100000))
                .createdAt(now)
                .updatedAt(now)
                .build();

        jobsById.put(jobId, job);
        if (orderDto.getId() != null) {
            jobIdByOrderId.put(orderDto.getId(), jobId);
        }
        pendingFutures.put(jobId, new CompletableFuture<>());
        queue.offer(job);
        persistJobsToFile();

        systemActivityService.addLog("QUEUE", "Tiếp nhận đơn vào hàng đợi",
                jobId + " | Đơn #" + orderDto.getId() + " (" + job.getPaymentMethod() + ") - Trạng thái: PENDING", "INFO");
        return job;
    }

    public OrderDTO awaitJobResultIfActive(String jobId, OrderDTO fallbackOrder, long timeoutMs) {
        if (workerPaused.get()) {
            return fallbackOrder;
        }
        CompletableFuture<OrderDTO> future = pendingFutures.get(jobId);
        if (future == null) {
            return fallbackOrder;
        }
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return fallbackOrder;
        }
    }

    @Scheduled(fixedDelay = 300)
    public void processQueueBatch() {
        if (workerPaused.get() || systemActivityService.isWriteLocked()) {
            return;
        }
        if (queue.isEmpty()) {
            return;
        }

        List<OrderJob> batch = new ArrayList<>();
        queue.drainTo(batch);
        if (batch.isEmpty()) return;

        // Xác định cấp độ phân xử nếu có >= 2 job trong cùng đợt
        for (int i = 0; i < batch.size(); i++) {
            OrderJob job = batch.get(i);
            if (batch.size() > 1) {
                OrderJob peer = (i == 0) ? batch.get(1) : batch.get(0);
                if (Math.abs(job.getEnqueuedAtMillis() - peer.getEnqueuedAtMillis()) <= 10) {
                    if (tierRank(job.getMembershipTier()) != tierRank(peer.getMembershipTier())) {
                        job.setResolutionTier("VIP_TIER");
                    } else {
                        job.setResolutionTier("FAIR_LOTTERY");
                    }
                } else {
                    job.setResolutionTier("FIFO");
                }
            }
            processSingleJob(job);
        }
    }

    private void processSingleJob(OrderJob job) {
        job.setStatus("PROCESSING");
        job.setProgress(30);
        job.setUpdatedAt(nowIso());
        persistJobsToFile();

        try {
            if (job.isSimulatedDlq()) {
                throw new BadRequestException("Lỗi giả lập kiểm thử Dead Letter Queue (DLQ)");
            }

            OrderDTO processedDto = null;
            if (job.getOrderId() != null) {
                processedDto = orderService.processQueuedOrderStock(job.getOrderId());
            }

            boolean isOnline = job.getPaymentMethod() != null &&
                    ("BANK_TRANSFER".equalsIgnoreCase(job.getPaymentMethod()) || "MOMO".equalsIgnoreCase(job.getPaymentMethod()));

            if (isOnline && job.getOrderId() != null) {
                job.setStatus("WAITING_PAYMENT");
                job.setProgress(80);
                job.setErrorMessage(null);
                job.setDeadLetter(false);
                job.setUpdatedAt(nowIso());
                systemActivityService.addLog("QUEUE", "Giữ kho thành công - Chờ duyệt thanh toán",
                        job.getJobId() + " | Đơn #" + job.getOrderId() + " [" + job.getResolutionTier() + "]", "INFO");
            } else {
                job.setStatus("SUCCEEDED");
                job.setProgress(100);
                job.setErrorMessage(null);
                job.setDeadLetter(false);
                job.setUpdatedAt(nowIso());
                systemActivityService.addLog("QUEUE", "Xử lý đơn thành công",
                        job.getJobId() + (job.getOrderId() != null ? (" | Đơn #" + job.getOrderId()) : " (Job thử nghiệm)")
                                + " [" + job.getResolutionTier() + "]", "SUCCESS");
            }

            persistJobsToFile();
            CompletableFuture<OrderDTO> fut = pendingFutures.remove(job.getJobId());
            if (fut != null && processedDto != null) {
                fut.complete(processedDto);
            }
        } catch (Exception ex) {
            String errMsg = ex.getMessage() != null ? ex.getMessage() : "Lỗi xử lý đơn hàng";
            if (job.getOrderId() != null) {
                orderService.markQueuedOrderFailed(job.getOrderId(), errMsg);
            }

            job.setStatus("FAILED");
            job.setProgress(100);
            job.setDeadLetter(true);
            job.setErrorMessage(errMsg);
            job.setUpdatedAt(nowIso());
            persistJobsToFile();

            systemActivityService.addLog("QUEUE", "Worker từ chối / Lỗi Job (DLQ)",
                    job.getJobId() + (job.getOrderId() != null ? (" | Đơn #" + job.getOrderId()) : "") + ": " + errMsg, "ERROR");

            CompletableFuture<OrderDTO> fut = pendingFutures.remove(job.getJobId());
            if (fut != null) {
                OrderDTO updated = null;
                try {
                    if (job.getOrderId() != null) {
                        updated = orderService.getOrderById(job.getOrderId());
                    }
                } catch (Exception ignored) {}
                fut.complete(updated);
            }
        }
    }

    public Map<String, Object> togglePause() {
        boolean next = !workerPaused.get();
        workerPaused.set(next);
        String msg = next
                ? "Đã TẠM DỪNG Worker xử lý hàng đợi. Đơn hàng mới sẽ ở trạng thái PENDING."
                : "Đã TIẾP TỤC chạy Worker! Các đơn đang chờ PENDING sẽ được xử lý ngay.";
        systemActivityService.addLog("QUEUE", next ? "Tạm dừng Worker" : "Tiếp tục Worker", msg, next ? "WARN" : "SUCCESS");
        return Map.of(
                "isPaused", next,
                "status", next ? "PAUSED" : "RUNNING",
                "message", msg
        );
    }

    public Map<String, Object> simulateFailureJob() {
        long seq = jobSeq.incrementAndGet();
        String jobId = "JOB-" + seq;
        String now = nowIso();

        OrderJob job = OrderJob.builder()
                .jobId(jobId)
                .orderId(null)
                .userId(null)
                .customerName("Đơn kiểm thử DLQ #" + seq)
                .membershipTier("GOLD")
                .paymentMethod("COD")
                .amount(BigDecimal.valueOf(150000))
                .status("PENDING")
                .progress(0)
                .retryCount(0)
                .deadLetter(false)
                .simulatedDlq(true)
                .resolutionTier("FIFO")
                .enqueuedAtMillis(System.currentTimeMillis())
                .lotteryScore(ThreadLocalRandom.current().nextInt(1, 100000))
                .createdAt(now)
                .updatedAt(now)
                .build();

        jobsById.put(jobId, job);

        if (workerPaused.get()) {
            // Khi đang Pause, đẩy thẳng vào FAILED/DLQ để admin thấy ngay tác vụ lỗi cần xử lý
            job.setStatus("FAILED");
            job.setProgress(100);
            job.setDeadLetter(true);
            job.setErrorMessage("Lỗi giả lập kiểm thử Dead Letter Queue (DLQ)");
            persistJobsToFile();
        } else {
            queue.offer(job);
            persistJobsToFile();
        }

        systemActivityService.addLog("QUEUE", "Tạo Job giả lập lỗi DLQ", jobId, "WARN");
        return Map.of("success", true, "job", job, "message", "Đã tạo Job giả lập lỗi " + jobId + " vào Dead Letter Queue!");
    }

    public Map<String, Object> retryJob(String jobId) {
        OrderJob job = jobsById.get(jobId);
        if (job == null) {
            throw new BadRequestException("Không tìm thấy Job: " + jobId);
        }
        if (!"FAILED".equals(job.getStatus())) {
            throw new BadRequestException("Chỉ có thể thử lại các Job đang ở trạng thái FAILED");
        }

        job.setRetryCount(job.getRetryCount() + 1);
        job.setStatus("RETRYING");
        job.setProgress(15);
        job.setDeadLetter(false);
        job.setSimulatedDlq(false);
        job.setErrorMessage(null);
        job.setEnqueuedAtMillis(System.currentTimeMillis());
        job.setLotteryScore(ThreadLocalRandom.current().nextInt(1, 100000));
        job.setUpdatedAt(nowIso());

        if (workerPaused.get()) {
            job.setStatus("PENDING");
            job.setProgress(0);
        }
        queue.offer(job);
        persistJobsToFile();

        systemActivityService.addLog("QUEUE", "Thử lại Job (Retry #" + job.getRetryCount() + ")",
                jobId + (job.getOrderId() != null ? (" | Đơn #" + job.getOrderId()) : ""), "INFO");

        return Map.of(
                "success", true,
                "job", job,
                "message", "Đã đưa Job " + jobId + " vào hàng đợi xử lý lại (Lần thử #" + job.getRetryCount() + ")!"
        );
    }

    public void deleteJob(String jobId) {
        OrderJob removed = jobsById.remove(jobId);
        if (removed == null) {
            throw new BadRequestException("Không tìm thấy Job: " + jobId);
        }
        if (removed.getOrderId() != null) {
            jobIdByOrderId.remove(removed.getOrderId());
        }
        queue.removeIf(j -> jobId.equals(j.getJobId()));
        persistJobsToFile();
        systemActivityService.addLog("QUEUE", "Xóa Job khỏi hàng đợi", jobId, "INFO");
    }

    public void markOrderPaymentSucceeded(Long orderId) {
        if (orderId == null) return;
        String jobId = jobIdByOrderId.get(orderId);
        if (jobId != null) {
            OrderJob job = jobsById.get(jobId);
            if (job != null && "WAITING_PAYMENT".equals(job.getStatus())) {
                job.setStatus("SUCCEEDED");
                job.setProgress(100);
                job.setUpdatedAt(nowIso());
                persistJobsToFile();
            }
        }
    }

    public void markOrderCancelledOrExpired(Long orderId, String reason) {
        if (orderId == null) return;
        String jobId = jobIdByOrderId.get(orderId);
        if (jobId != null) {
            OrderJob job = jobsById.get(jobId);
            if (job != null && !"FAILED".equals(job.getStatus())) {
                job.setStatus("FAILED");
                job.setProgress(100);
                job.setDeadLetter(true);
                job.setErrorMessage(reason);
                job.setUpdatedAt(nowIso());
                persistJobsToFile();
            }
        }
    }

    public Map<String, Object> getSummary() {
        long pendingCount = 0;
        long processingCount = 0;
        long waitingPaymentCount = 0;
        long succeededCount = 0;
        long failedCount = 0;
        long retryCountTotal = 0;

        for (OrderJob j : jobsById.values()) {
            switch (j.getStatus()) {
                case "PENDING" -> pendingCount++;
                case "PROCESSING", "RETRYING" -> processingCount++;
                case "WAITING_PAYMENT" -> waitingPaymentCount++;
                case "SUCCEEDED" -> succeededCount++;
                case "FAILED" -> failedCount++;
            }
            if (j.getRetryCount() > 0) {
                retryCountTotal++;
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("streamLength", queue.size());
        summary.put("deadLetterLength", failedCount);
        summary.put("retryLength", retryCountTotal);
        summary.put("isPaused", workerPaused.get());
        summary.put("status", workerPaused.get() ? "PAUSED" : "RUNNING");
        summary.put("dbPendingCount", pendingCount);
        summary.put("dbProcessingCount", processingCount);
        summary.put("waitingPaymentCount", waitingPaymentCount);
        summary.put("dbSucceededCount", succeededCount);
        summary.put("dbFailedCount", failedCount);
        summary.put("totalJobs", jobsById.size());
        return summary;
    }

    public Map<String, Object> getJobsPage(String status, String keyword, int page, int size) {
        List<OrderJob> filtered = jobsById.values().stream()
                .filter(j -> status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)
                        || status.equalsIgnoreCase(j.getStatus()))
                .filter(j -> {
                    if (keyword == null || keyword.isBlank()) return true;
                    String kw = keyword.trim().toLowerCase();
                    return (j.getJobId() != null && j.getJobId().toLowerCase().contains(kw))
                            || (j.getCustomerName() != null && j.getCustomerName().toLowerCase().contains(kw))
                            || (j.getOrderId() != null && String.valueOf(j.getOrderId()).contains(kw))
                            || (j.getErrorMessage() != null && j.getErrorMessage().toLowerCase().contains(kw));
                })
                .sorted(Comparator.comparingLong(OrderJob::getEnqueuedAtMillis).reversed())
                .collect(Collectors.toList());

        int safeSize = size <= 0 ? 15 : size;
        int safePage = Math.max(page, 0);
        int totalElements = filtered.size();
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);
        int from = Math.min(safePage * safeSize, totalElements);
        int to = Math.min(from + safeSize, totalElements);

        List<OrderJob> content = filtered.subList(from, to);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("content", content);
        res.put("number", safePage);
        res.put("size", safeSize);
        res.put("totalElements", totalElements);
        res.put("totalPages", totalPages);
        return res;
    }

    private void loadJobsFromFile() {
        synchronized (fileLock) {
            try {
                File file = new File(JOBS_FILE_PATH);
                if (file.exists() && file.length() > 0) {
                    List<OrderJob> loaded = objectMapper.readValue(file, new TypeReference<List<OrderJob>>() {});
                    if (loaded != null) {
                        long maxSeq = 1000;
                        for (OrderJob j : loaded) {
                            jobsById.put(j.getJobId(), j);
                            if (j.getOrderId() != null) {
                                jobIdByOrderId.put(j.getOrderId(), j.getJobId());
                            }
                            if (j.getJobId() != null && j.getJobId().startsWith("JOB-")) {
                                try {
                                    long num = Long.parseLong(j.getJobId().substring(4));
                                    if (num > maxSeq) maxSeq = num;
                                } catch (NumberFormatException ignored) {}
                            }
                            if ("PENDING".equals(j.getStatus()) || "RETRYING".equals(j.getStatus()) || "PROCESSING".equals(j.getStatus())) {
                                j.setStatus("PENDING");
                                j.setProgress(0);
                                queue.offer(j);
                            }
                        }
                        jobSeq.set(maxSeq);
                    }
                }
            } catch (Exception e) {
                log.warn("Không thể nạp lịch sử Job từ {}: {}", JOBS_FILE_PATH, e.getMessage());
            }
        }
    }

    private void persistJobsToFile() {
        synchronized (fileLock) {
            try {
                Path path = Paths.get(JOBS_FILE_PATH);
                if (path.getParent() != null) {
                    Files.createDirectories(path.getParent());
                }
                List<OrderJob> snapshot = jobsById.values().stream()
                        .sorted(Comparator.comparingLong(OrderJob::getEnqueuedAtMillis).reversed())
                        .limit(300)
                        .collect(Collectors.toList());
                objectMapper.writeValue(path.toFile(), snapshot);
            } catch (Exception e) {
                log.warn("Không thể lưu lịch sử Job ra {}: {}", JOBS_FILE_PATH, e.getMessage());
            }
        }
    }
}

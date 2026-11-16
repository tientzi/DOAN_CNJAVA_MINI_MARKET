package com.groceryshop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SystemActivityService {

    private static final Logger log = LoggerFactory.getLogger(SystemActivityService.class);
    private static final int MAX_LOGS = 200;
    private static final String LOG_FILE_PATH = "backups/.activity-logs.json";

    private final Deque<ActivityLogEntry> logs = new ConcurrentLinkedDeque<>();
    private final AtomicBoolean writeLocked = new AtomicBoolean(false);
    private volatile String lockReason = "";
    private final Object fileLock = new Object();

    private final ObjectMapper objectMapper;

    public SystemActivityService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityLogEntry {
        private String id;
        private String timestamp;
        private String type;   // SYSTEM, BACKUP, RESTORE, QUEUE, EXPIRY, IDEMPOTENCY
        private String action;
        private String detail;
        private String status; // INFO, SUCCESS, WARN, ERROR
    }

    @PostConstruct
    public void init() {
        loadLogsFromFile();
        addLog("SYSTEM", "Khởi động hệ thống", "SystemActivityService đã sẵn sàng theo dõi nhật ký và khóa ghi toàn cục", "INFO");
    }

    public boolean isWriteLocked() {
        return writeLocked.get();
    }

    public String getLockReason() {
        return lockReason;
    }

    public boolean tryAcquireWriteLock(String reason) {
        if (writeLocked.compareAndSet(false, true)) {
            this.lockReason = reason != null ? reason : "Hệ thống đang bảo trì dữ liệu";
            addLog("SYSTEM", "Khóa ghi dữ liệu toàn cục", this.lockReason, "WARN");
            return true;
        }
        return false;
    }

    public void forceAcquireWriteLock(String reason) {
        writeLocked.set(true);
        this.lockReason = reason != null ? reason : "Hệ thống đang bảo trì dữ liệu";
        addLog("SYSTEM", "Khóa ghi dữ liệu toàn cục", this.lockReason, "WARN");
    }

    public void releaseWriteLock() {
        if (writeLocked.compareAndSet(true, false)) {
            String prevReason = this.lockReason;
            this.lockReason = "";
            addLog("SYSTEM", "Mở khóa ghi hệ thống", "Hoàn tất tác vụ: " + prevReason, "SUCCESS");
        }
    }

    public void addLog(String type, String action, String detail, String status) {
        ActivityLogEntry entry = ActivityLogEntry.builder()
                .id(UUID.randomUUID().toString().substring(0, 8))
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .type(type != null ? type : "SYSTEM")
                .action(action != null ? action : "")
                .detail(detail != null ? detail : "")
                .status(status != null ? status : "INFO")
                .build();

        logs.addFirst(entry);
        while (logs.size() > MAX_LOGS) {
            logs.pollLast();
        }
        persistLogsToFile();
    }

    public List<ActivityLogEntry> getLogs(int limit) {
        int safeLimit = limit <= 0 ? 50 : Math.min(limit, MAX_LOGS);
        List<ActivityLogEntry> result = new ArrayList<>(safeLimit);
        int count = 0;
        for (ActivityLogEntry entry : logs) {
            if (count >= safeLimit) break;
            result.add(entry);
            count++;
        }
        return result;
    }

    private void loadLogsFromFile() {
        synchronized (fileLock) {
            try {
                File file = new File(LOG_FILE_PATH);
                if (file.exists() && file.length() > 0) {
                    List<ActivityLogEntry> loaded = objectMapper.readValue(file, new TypeReference<List<ActivityLogEntry>>() {});
                    if (loaded != null) {
                        logs.clear();
                        int count = 0;
                        for (ActivityLogEntry e : loaded) {
                            if (count >= MAX_LOGS) break;
                            logs.addLast(e);
                            count++;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Không thể nạp file nhật ký hoạt động {}: {}", LOG_FILE_PATH, e.getMessage());
            }
        }
    }

    private void persistLogsToFile() {
        synchronized (fileLock) {
            try {
                Path path = Paths.get(LOG_FILE_PATH);
                if (path.getParent() != null) {
                    Files.createDirectories(path.getParent());
                }
                List<ActivityLogEntry> snapshot = new ArrayList<>(logs);
                objectMapper.writeValue(path.toFile(), snapshot);
            } catch (Exception e) {
                log.warn("Không thể lưu file nhật ký hoạt động {}: {}", LOG_FILE_PATH, e.getMessage());
            }
        }
    }
}

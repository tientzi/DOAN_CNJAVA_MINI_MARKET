package com.groceryshop.service;

import com.groceryshop.dto.OrderDTO;
import com.groceryshop.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class IdempotencyService {

    private static final long KEY_TTL_MS = 10 * 60 * 1000L;     // 10 phút cho X-Idempotency-Key
    private static final long USER_GUARD_TTL_MS = 5 * 1000L;    // 5 giây chống bấm đúp theo User

    private static class CacheEntry {
        volatile boolean inFlight;
        volatile OrderDTO result;
        volatile long expiresAt;

        CacheEntry(boolean inFlight, OrderDTO result, long expiresAt) {
            this.inFlight = inFlight;
            this.result = result;
            this.expiresAt = expiresAt;
        }
    }

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Autowired
    private SystemActivityService systemActivityService;

    public OrderDTO executeOrderWithIdempotency(Long userId,
                                                String idempotencyKey,
                                                OrderDTO requestDto,
                                                Supplier<OrderDTO> executor) {
        long now = System.currentTimeMillis();

        String normalizedHeaderKey = (idempotencyKey != null && !idempotencyKey.trim().isEmpty())
                ? "key:" + userId + ":" + idempotencyKey.trim()
                : null;

        String paymentMethod = requestDto != null && requestDto.getPaymentMethod() != null
                ? requestDto.getPaymentMethod() : "COD";
        String userGuardKey = "guard:" + userId + ":" + paymentMethod;

        // 1. Kiểm tra X-Idempotency-Key nếu có
        if (normalizedHeaderKey != null) {
            CacheEntry existing = cache.get(normalizedHeaderKey);
            if (existing != null && existing.expiresAt > now) {
                if (existing.inFlight) {
                    systemActivityService.addLog("IDEMPOTENCY", "Chặn gửi trùng (in-flight)",
                            "User #" + userId + " gửi trùng key=" + idempotencyKey, "WARN");
                    throw new BadRequestException("Đơn hàng đang được xử lý, vui lòng không bấm Đặt hàng nhiều lần!");
                }
                if (existing.result != null) {
                    systemActivityService.addLog("IDEMPOTENCY", "Trả về đơn hàng cũ (Idempotent hit)",
                            "User #" + userId + " -> Đơn #" + existing.result.getId() + " (key=" + idempotencyKey + ")", "INFO");
                    return existing.result;
                }
            }
        }

        // 2. Kiểm tra khóa chống bấm đúp theo User trong 5 giây
        CacheEntry guardExisting = cache.get(userGuardKey);
        if (guardExisting != null && guardExisting.expiresAt > now) {
            if (guardExisting.inFlight) {
                systemActivityService.addLog("IDEMPOTENCY", "Chặn bấm đúp trong 5s (in-flight)",
                        "User #" + userId + " đang có đơn xử lý", "WARN");
                throw new BadRequestException("Hệ thống đang xử lý đơn hàng của bạn, vui lòng đợi giây lát!");
            }
            if (guardExisting.result != null) {
                systemActivityService.addLog("IDEMPOTENCY", "Chặn bấm đúp trong 5s (Trả đơn cũ)",
                        "User #" + userId + " -> Đơn #" + guardExisting.result.getId(), "WARN");
                return guardExisting.result;
            }
        }

        // Đánh dấu đang xử lý (atomic putIfAbsent cho guard key)
        CacheEntry inFlightEntry = new CacheEntry(true, null, now + USER_GUARD_TTL_MS);
        CacheEntry raceGuard = cache.putIfAbsent(userGuardKey, inFlightEntry);
        if (raceGuard != null && raceGuard.expiresAt > now) {
            throw new BadRequestException("Yêu cầu đặt hàng đang được xử lý, vui lòng không bấm lặp lại!");
        }
        cache.put(userGuardKey, inFlightEntry);

        CacheEntry keyEntry = null;
        if (normalizedHeaderKey != null) {
            keyEntry = new CacheEntry(true, null, now + KEY_TTL_MS);
            cache.put(normalizedHeaderKey, keyEntry);
        }

        try {
            OrderDTO createdOrder = executor.get();
            long doneTime = System.currentTimeMillis();

            inFlightEntry.inFlight = false;
            inFlightEntry.result = createdOrder;
            inFlightEntry.expiresAt = doneTime + USER_GUARD_TTL_MS;

            if (keyEntry != null) {
                keyEntry.inFlight = false;
                keyEntry.result = createdOrder;
                keyEntry.expiresAt = doneTime + KEY_TTL_MS;
            }
            return createdOrder;
        } catch (RuntimeException ex) {
            // Nếu đặt hàng lỗi thì giải phóng khóa để user có thể sửa và đặt lại ngay
            cache.remove(userGuardKey);
            if (normalizedHeaderKey != null) {
                cache.remove(normalizedHeaderKey);
            }
            throw ex;
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
    }
}

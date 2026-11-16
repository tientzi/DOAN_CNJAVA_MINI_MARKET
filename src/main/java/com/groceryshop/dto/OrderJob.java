package com.groceryshop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderJob {
    private String jobId;
    private Long orderId;
    private Long userId;
    private String customerName;
    private String membershipTier; // DIAMOND | GOLD | SILVER | BRONZE
    private String paymentMethod;  // COD | BANK_TRANSFER | MOMO
    private BigDecimal amount;

    private String status;         // PENDING | PROCESSING | WAITING_PAYMENT | SUCCEEDED | FAILED | RETRYING
    private int progress;          // 0 | 15 | 30 | 80 | 100
    private int retryCount;
    private boolean deadLetter;
    private boolean simulatedDlq;
    private String resolutionTier; // FIFO | VIP_TIER | FAIR_LOTTERY
    private String errorMessage;

    private long enqueuedAtMillis;
    private int lotteryScore;
    private String createdAt;
    private String updatedAt;
}

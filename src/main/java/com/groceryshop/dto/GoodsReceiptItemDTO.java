package com.groceryshop.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptItemDTO {
    private Long id;
    private Long productId;
    private String productName;
    private String sku;
    private Integer quantity;
    private BigDecimal importPrice;
    private String batchName;
    private LocalDate expiryDate;
}

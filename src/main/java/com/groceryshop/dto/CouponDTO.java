package com.groceryshop.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponDTO {
    private Long id;

    @NotBlank(message = "Mã giảm giá không được để trống")
    @Size(max = 50, message = "Mã giảm giá tối đa 50 ký tự")
    private String code;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;

    @NotBlank(message = "Vui lòng chọn loại chiết khấu")
    private String discountType;

    @NotNull(message = "Vui lòng nhập giá trị giảm")
    @DecimalMin(value = "0.01", message = "Giá trị giảm phải lớn hơn 0")
    private BigDecimal discountValue;

    @NotNull(message = "Vui lòng chọn ngày bắt đầu hiệu lực")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startDate;

    @NotNull(message = "Vui lòng chọn ngày hết hạn hiệu lực")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endDate;

    @NotNull(message = "Vui lòng nhập giá trị đơn tối thiểu")
    @DecimalMin(value = "0.0", message = "Giá trị đơn tối thiểu không được âm")
    private BigDecimal minOrderAmount;

    private Integer usedCount;

    @NotNull(message = "Vui lòng nhập số lượt sử dụng tối đa")
    @Min(value = 1, message = "Số lượt sử dụng tối đa phải từ 1 trở lên")
    private Integer maxUses;

    private Boolean isActive;
    private Long applicableCategoryId;
    private String applicableCategoryName;
    private BigDecimal maxDiscountAmount;
}

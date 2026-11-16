package com.groceryshop.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierDTO {
    private Long id;

    @NotBlank(message = "Tên nhà cung cấp không được để trống")
    private String name;

    private String contactName;

    @Pattern(regexp = "^$|^[0-9+\\-\\s]{8,15}$", message = "Số điện thoại không hợp lệ (8-15 chữ số)")
    private String phone;

    @Email(message = "Email không đúng định dạng")
    private String email;

    private String address;
    private Boolean isActive;
    private LocalDateTime createdAt;
}

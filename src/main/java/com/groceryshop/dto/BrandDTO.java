package com.groceryshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandDTO {
    private Long id;

    @NotBlank(message = "Tên thương hiệu không được để trống")
    @Size(max = 100, message = "Tên thương hiệu tối đa 100 ký tự")
    private String name;

    @Size(max = 500, message = "Mô tả thương hiệu tối đa 500 ký tự")
    private String description;

    private Boolean isActive;
}

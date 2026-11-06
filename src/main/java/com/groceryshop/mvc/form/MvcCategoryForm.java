package com.groceryshop.mvc.form;

import com.groceryshop.dto.CategoryDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MvcCategoryForm {
    private Long id;

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(min = 2, max = 100, message = "Tên danh mục phải từ 2 đến 100 ký tự")
    private String name;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;

    private String image;

    @Builder.Default
    private Boolean isActive = true;

    public static MvcCategoryForm fromDTO(CategoryDTO dto) {
        return MvcCategoryForm.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .image(dto.getImage())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();
    }

    public CategoryDTO toDTO() {
        return CategoryDTO.builder()
                .id(this.id)
                .name(this.name != null ? this.name.trim() : null)
                .description(this.description != null ? this.description.trim() : null)
                .image(this.image != null && !this.image.trim().isEmpty() ? this.image.trim() : "/uploads/cat_rau_cu_qua.jpg")
                .isActive(this.isActive != null ? this.isActive : true)
                .build();
    }
}

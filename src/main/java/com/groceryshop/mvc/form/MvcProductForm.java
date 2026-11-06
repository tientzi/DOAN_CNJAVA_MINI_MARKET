package com.groceryshop.mvc.form;

import com.groceryshop.dto.ProductDTO;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MvcProductForm {
    private Long id;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255, message = "Tên sản phẩm tối đa 255 ký tự")
    private String name;

    @NotBlank(message = "Mã SKU không được để trống")
    @Size(max = 50, message = "Mã SKU tối đa 50 ký tự")
    private String sku;

    @Size(max = 100, message = "Mã vạch tối đa 100 ký tự")
    private String barcode;

    @NotNull(message = "Vui lòng chọn danh mục sản phẩm")
    private Long categoryId;

    @NotNull(message = "Vui lòng chọn thương hiệu")
    private Long brandId;

    @NotNull(message = "Giá bán niêm yết không được để trống")
    @DecimalMin(value = "1000.0", message = "Giá bán niêm yết phải từ 1.000đ trở lên")
    private BigDecimal price;

    @DecimalMin(value = "0.0", message = "Giá khuyến mãi không được âm")
    private BigDecimal salePrice;

    @NotBlank(message = "Đơn vị tính không được để trống (VD: Hộp, Chai, Kg)")
    private String unit;

    @Min(value = 0, message = "Trọng lượng (gram) không được âm")
    private Integer weightG;

    @Min(value = 0, message = "Ngưỡng tồn kho tối thiểu không được âm")
    private Integer minimumStock;

    private String location;

    private String mainImage;

    private String description;

    @Builder.Default
    private Boolean isActive = true;

    private Integer currentStock;

    public static MvcProductForm fromDTO(ProductDTO dto) {
        return MvcProductForm.builder()
                .id(dto.getId())
                .name(dto.getName())
                .sku(dto.getSku())
                .barcode(dto.getBarcode())
                .categoryId(dto.getCategoryId())
                .brandId(dto.getBrandId())
                .price(dto.getPrice())
                .salePrice(dto.getSalePrice())
                .unit(dto.getUnit() != null ? dto.getUnit() : "Cái")
                .weightG(dto.getWeightG() != null ? dto.getWeightG() : 0)
                .minimumStock(dto.getMinimumStock() != null ? dto.getMinimumStock() : 5)
                .location(dto.getLocation() != null ? dto.getLocation() : "Kệ A1")
                .mainImage(dto.getMainImage())
                .description(dto.getDescription())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .currentStock(dto.getCurrentStock() != null ? dto.getCurrentStock() : 0)
                .build();
    }

    public ProductDTO toDTO() {
        return ProductDTO.builder()
                .id(this.id)
                .name(this.name != null ? this.name.trim() : null)
                .sku(this.sku != null ? this.sku.trim() : null)
                .barcode(this.barcode != null ? this.barcode.trim() : null)
                .categoryId(this.categoryId)
                .brandId(this.brandId)
                .price(this.price)
                .salePrice(this.salePrice)
                .unit(this.unit != null ? this.unit.trim() : "Cái")
                .weightG(this.weightG != null ? this.weightG : 0)
                .minimumStock(this.minimumStock != null ? this.minimumStock : 5)
                .location(this.location != null && !this.location.trim().isEmpty() ? this.location.trim() : "Kệ mặc định")
                .mainImage(this.mainImage != null && !this.mainImage.trim().isEmpty() ? this.mainImage.trim() : "/uploads/xa_lach_thuy_canh.jpg")
                .description(this.description)
                .isActive(this.isActive != null ? this.isActive : true)
                .build();
    }
}

package com.groceryshop.mvc.form;

import com.groceryshop.dto.SupplierDTO;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MvcSupplierForm {
    private Long id;

    @NotBlank(message = "Tên nhà cung cấp không được để trống")
    @Size(min = 3, max = 255, message = "Tên nhà cung cấp phải từ 3 đến 255 ký tự")
    private String name;

    @NotBlank(message = "Tên người liên hệ không được để trống")
    @Size(max = 100, message = "Tên người liên hệ tối đa 100 ký tự")
    private String contactName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0\\d{9,10}$", message = "Số điện thoại phải bắt đầu bằng số 0 và gồm 10–11 chữ số")
    private String phone;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Định dạng địa chỉ Email không hợp lệ")
    private String email;

    @NotBlank(message = "Địa chỉ kho cung ứng không được để trống")
    @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
    private String address;

    @Builder.Default
    private Boolean isActive = true;

    public static MvcSupplierForm fromDTO(SupplierDTO dto) {
        return MvcSupplierForm.builder()
                .id(dto.getId())
                .name(dto.getName())
                .contactName(dto.getContactName())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .address(dto.getAddress())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();
    }

    public SupplierDTO toDTO() {
        return SupplierDTO.builder()
                .id(this.id)
                .name(this.name != null ? this.name.trim() : null)
                .contactName(this.contactName != null ? this.contactName.trim() : null)
                .phone(this.phone != null ? this.phone.trim() : null)
                .email(this.email != null ? this.email.trim() : null)
                .address(this.address != null ? this.address.trim() : null)
                .isActive(this.isActive != null ? this.isActive : true)
                .build();
    }
}

package com.groceryshop.service;

import com.groceryshop.dto.SupplierDTO;
import com.groceryshop.entity.Supplier;
import com.groceryshop.exception.BadRequestException;
import com.groceryshop.exception.ResourceNotFoundException;
import com.groceryshop.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    public List<SupplierDTO> getAllSuppliers() {
        return supplierRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public SupplierDTO getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà cung cấp"));
        return toDTO(supplier);
    }

    @Transactional
    public SupplierDTO createSupplier(SupplierDTO dto) {
        if (supplierRepository.findByName(dto.getName()).isPresent()) {
            throw new BadRequestException("Tên nhà cung cấp đã tồn tại");
        }
        Supplier supplier = Supplier.builder()
                .name(dto.getName())
                .contactName(dto.getContactName())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .address(dto.getAddress())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();
        return toDTO(supplierRepository.save(supplier));
    }

    @Transactional
    public SupplierDTO updateSupplier(Long id, SupplierDTO dto) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà cung cấp"));
                
        supplierRepository.findByName(dto.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) throw new BadRequestException("Tên nhà cung cấp đã tồn tại");
        });

        supplier.setName(dto.getName());
        supplier.setContactName(dto.getContactName());
        supplier.setPhone(dto.getPhone());
        supplier.setEmail(dto.getEmail());
        supplier.setAddress(dto.getAddress());
        if (dto.getIsActive() != null) {
            supplier.setIsActive(dto.getIsActive());
        }

        return toDTO(supplierRepository.save(supplier));
    }

    @Transactional
    public void deleteSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà cung cấp"));
        supplierRepository.delete(supplier);
    }

    private SupplierDTO toDTO(Supplier supplier) {
        return com.groceryshop.mapper.EntityMapper.toSupplierDTO(supplier);
    }
}

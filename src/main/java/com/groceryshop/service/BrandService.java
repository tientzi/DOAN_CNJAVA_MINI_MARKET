package com.groceryshop.service;

import com.groceryshop.dto.BrandDTO;
import com.groceryshop.entity.Brand;
import com.groceryshop.exception.BadRequestException;
import com.groceryshop.exception.ResourceNotFoundException;
import com.groceryshop.mapper.EntityMapper;
import com.groceryshop.repository.BrandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BrandService {

    @Autowired
    private BrandRepository brandRepository;

    public List<BrandDTO> getAllBrands() {
        return brandRepository.findAll().stream()
                .map(EntityMapper::toBrandDTO)
                .collect(Collectors.toList());
    }

    public Page<BrandDTO> getPaged(String keyword, String sortBy, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        if ("newest".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "id");
        } else if ("name_asc".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "name");
        } else if ("name_desc".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "name");
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), size, sort);
        String kw = keyword == null ? "" : keyword.trim();
        return brandRepository.findByNameContainingIgnoreCase(kw, pageable)
                .map(EntityMapper::toBrandDTO);
    }

    public List<BrandDTO> getActiveBrands() {
        return brandRepository.findByIsActiveTrue().stream()
                .map(EntityMapper::toBrandDTO)
                .collect(Collectors.toList());
    }

    public BrandDTO getBrandById(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu với id: " + id));
        return EntityMapper.toBrandDTO(brand);
    }

    @Transactional
    public BrandDTO createBrand(BrandDTO dto) {
        if (brandRepository.findByName(dto.getName()).isPresent()) {
            throw new BadRequestException("Tên thương hiệu đã tồn tại");
        }
        Brand brand = EntityMapper.toBrandEntity(dto);
        if (brand.getIsActive() == null) brand.setIsActive(true);
        Brand saved = brandRepository.save(brand);
        return EntityMapper.toBrandDTO(saved);
    }

    @Transactional
    public BrandDTO updateBrand(Long id, BrandDTO dto) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu với id: " + id));
        brandRepository.findByName(dto.getName())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new BadRequestException("Tên thương hiệu đã được sử dụng");
                    }
                });

        brand.setName(dto.getName());
        brand.setDescription(dto.getDescription());
        if (dto.getIsActive() != null) {
            brand.setIsActive(dto.getIsActive());
        }
        Brand updated = brandRepository.save(brand);
        return EntityMapper.toBrandDTO(updated);
    }

    @Transactional
    public BrandDTO toggleBrandStatus(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu với id: " + id));
        boolean current = brand.getIsActive() == null || brand.getIsActive();
        brand.setIsActive(!current);
        return EntityMapper.toBrandDTO(brandRepository.save(brand));
    }

    @Transactional
    public void deleteBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu với id: " + id));
        try {
            brandRepository.delete(brand);
            brandRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("Không thể xóa thương hiệu này vì đang có sản phẩm thuộc thương hiệu");
        }
    }
}

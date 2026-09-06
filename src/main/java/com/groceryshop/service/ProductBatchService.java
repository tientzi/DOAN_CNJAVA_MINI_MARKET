package com.groceryshop.service;

import com.groceryshop.dto.ProductBatchDTO;
import com.groceryshop.entity.Product;
import com.groceryshop.entity.ProductBatch;
import com.groceryshop.exception.BadRequestException;
import com.groceryshop.exception.ResourceNotFoundException;
import com.groceryshop.mapper.EntityMapper;
import com.groceryshop.repository.ProductBatchRepository;
import com.groceryshop.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductBatchService {

    private final ProductBatchRepository batchRepository;
    private final ProductRepository productRepository;

    public ProductBatchService(ProductBatchRepository batchRepository, ProductRepository productRepository) {
        this.batchRepository = batchRepository;
        this.productRepository = productRepository;
    }

    public List<ProductBatchDTO> getAllBatches() {
        return batchRepository.findAll().stream()
                .filter(b -> b.getQuantity() != null && b.getQuantity() > 0)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<ProductBatchDTO> getExpiringBatches(int days) {
        LocalDate threshold = LocalDate.now().plusDays(days);
        return batchRepository.findAll().stream()
                .filter(b -> b.getQuantity() != null && b.getQuantity() > 0 
                        && b.getExpiryDate() != null && !b.getExpiryDate().isAfter(threshold))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void applyClearanceSale(Long productId, Integer discountPercentage, Double customSalePrice) {
        if (productId == null) {
            throw new BadRequestException("ID sản phẩm không được để trống");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại với id: " + productId));

        BigDecimal currentPrice = product.getPrice();
        BigDecimal salePrice;

        if (customSalePrice != null && customSalePrice > 0) {
            salePrice = BigDecimal.valueOf(customSalePrice);
        } else if (discountPercentage != null && discountPercentage > 0 && discountPercentage < 100) {
            salePrice = currentPrice.multiply(BigDecimal.valueOf(100 - discountPercentage))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        } else {
            throw new BadRequestException("Vui lòng nhập % chiết khấu hợp lệ (1 - 99%) hoặc giá sale mong muốn");
        }

        if (salePrice.compareTo(currentPrice) >= 0) {
            throw new BadRequestException("Giá xả kho phải nhỏ hơn giá gốc hiện tại (" + currentPrice + "đ)");
        }

        product.setSalePrice(salePrice);
        productRepository.save(product);
    }

    @Transactional
    public void applyClearanceSale(Long productId, Integer discountPercentage) {
        applyClearanceSale(productId, discountPercentage, null);
    }

    private ProductBatchDTO toDTO(ProductBatch batch) {
        return EntityMapper.toProductBatchDTO(batch);
    }
}

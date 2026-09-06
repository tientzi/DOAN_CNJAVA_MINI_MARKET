package com.groceryshop.repository;

import com.groceryshop.entity.ProductBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductBatchRepository extends JpaRepository<ProductBatch, Long> {
    List<ProductBatch> findByProductIdAndQuantityGreaterThanOrderByExpiryDateAsc(Long productId, int quantity);
}

package com.groceryshop.repository;

import com.groceryshop.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByProductId(Long productId);

    /**
     * Khóa bi quan (Pessimistic Write Lock) để bảo vệ tài nguyên chia sẻ tồn kho
     * Ngăn chặn hiện tượng Race Condition (bán âm hàng) khi nhiều khách mua cùng lúc
     * Database: SQL Server kích hoạt khóa UPDLOCK, ROWLOCK trong giao dịch
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId")
    Optional<Inventory> findByProductIdWithLock(@Param("productId") Long productId);

    @Query("SELECT i FROM Inventory i WHERE i.currentStock < i.minimumStock")
    List<Inventory> findLowStockWarnings();
}
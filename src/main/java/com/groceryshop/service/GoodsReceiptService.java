package com.groceryshop.service;

import com.groceryshop.dto.GoodsReceiptDTO;
import com.groceryshop.dto.GoodsReceiptItemDTO;
import com.groceryshop.entity.*;
import com.groceryshop.exception.BadRequestException;
import com.groceryshop.exception.ResourceNotFoundException;
import com.groceryshop.repository.ProductBatchRepository;
import com.groceryshop.repository.GoodsReceiptItemRepository;
import com.groceryshop.repository.GoodsReceiptRepository;
import com.groceryshop.repository.InventoryRepository;
import com.groceryshop.repository.ProductRepository;
import com.groceryshop.repository.SupplierRepository;
import com.groceryshop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.stream.Collectors;

@Service
public class GoodsReceiptService {

    @Autowired
    private GoodsReceiptRepository receiptRepository;

    @Autowired
    private GoodsReceiptItemRepository itemRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InventoryLedgerService ledgerService;

    @Autowired
    private ProductBatchRepository productBatchRepository;

    public Page<GoodsReceiptDTO> getAllReceipts(Pageable pageable) {
        return receiptRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toDTO);
    }

    public GoodsReceiptDTO getReceiptById(Long id) {
        GoodsReceipt receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu nhập"));
        return toDTO(receipt);
    }

    @Transactional
    public GoodsReceiptDTO createReceipt(Long userId, GoodsReceiptDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user"));

        Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà cung cấp"));

        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BadRequestException("Phiếu nhập phải có ít nhất 1 sản phẩm");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        GoodsReceipt receipt = GoodsReceipt.builder()
                .supplier(supplier)
                .totalAmount(totalAmount)
                .note(dto.getNote())
                .status("DRAFT")
                .createdBy(user)
                .build();
                
        GoodsReceipt savedReceipt = receiptRepository.save(receipt);

        for (GoodsReceiptItemDTO itemDTO : dto.getItems()) {
            Product product = productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm id: " + itemDTO.getProductId()));
            
            GoodsReceiptItem item = GoodsReceiptItem.builder()
                    .goodsReceipt(savedReceipt)
                    .product(product)
                    .quantity(itemDTO.getQuantity())
                    .importPrice(itemDTO.getImportPrice())
                    .batchName(itemDTO.getBatchName())
                    .expiryDate(itemDTO.getExpiryDate())
                    .build();
                    
            itemRepository.save(item);
            savedReceipt.getItems().add(item);

            totalAmount = totalAmount.add(itemDTO.getImportPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));
        }

        savedReceipt.setTotalAmount(totalAmount);
        return toDTO(receiptRepository.save(savedReceipt));
    }

    @Transactional
    public GoodsReceiptDTO completeReceipt(Long userId, Long receiptId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user"));

        GoodsReceipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu nhập"));

        if (!receipt.getStatus().equals("DRAFT")) {
            throw new BadRequestException("Chỉ có thể hoàn thành phiếu nhập ở trạng thái DRAFT");
        }

        receipt.setStatus("COMPLETED");

        for (GoodsReceiptItem item : receipt.getItems()) {
            Product product = item.getProduct();
            Inventory inventory = product.getInventory();
            if (inventory == null) {
                inventory = Inventory.builder().product(product).currentStock(0).minimumStock(5).build();
            }
            
            inventory.setCurrentStock(inventory.getCurrentStock() + item.getQuantity());
            inventoryRepository.save(inventory);

            ledgerService.recordLog(
                    inventory,
                    "IMPORT",
                    item.getQuantity(),
                    receipt.getId(),
                    "Nhập hàng từ phiếu #" + receipt.getId(),
                    user
            );

            if (item.getBatchName() != null && item.getExpiryDate() != null) {
                ProductBatch batch = ProductBatch.builder()
                        .product(product)
                        .goodsReceipt(receipt)
                        .batchName(item.getBatchName())
                        .quantity(item.getQuantity())
                        .expiryDate(item.getExpiryDate())
                        .build();
                productBatchRepository.save(batch);
            }
        }

        return toDTO(receiptRepository.save(receipt));
    }

    @Transactional
    public void cancelReceipt(Long receiptId) {
        GoodsReceipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu nhập"));
                
        if (!receipt.getStatus().equals("DRAFT")) {
            throw new BadRequestException("Chỉ có thể hủy phiếu nhập ở trạng thái DRAFT");
        }
        
        receipt.setStatus("CANCELLED");
        receiptRepository.save(receipt);
    }

    private GoodsReceiptDTO toDTO(GoodsReceipt receipt) {
        return com.groceryshop.mapper.EntityMapper.toGoodsReceiptDTO(receipt);
    }
}

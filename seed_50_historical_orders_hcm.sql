-- =========================================================================================
-- SUPERMARKET MINI MART - SCRIPT ĐỒNG BỘ ĐỊA CHỈ & SHIPPER 3 QUẬN TP.HCM
-- Khu vực: Quận Tân Phú (shipper1), Quận Tân Bình (shipper2), Quận 12 (shipper3)
-- =========================================================================================

USE [supermarket_db1];
GO

PRINT N'=== BẮT ĐẦU CẬP NHẬT THÔNG TIN SHIPPER & KHÁCH HÀNG VIP ===';

-- 1. Cập nhật thông tin phân công Shipper cố định theo 3 quận
UPDATE [users] 
SET [full_name] = N'Nguyễn Văn Giao (Quận Tân Phú)'
WHERE [username] = 'shipper1';

UPDATE [users] 
SET [full_name] = N'Trần Văn Tốc (Quận Tân Bình)'
WHERE [username] = 'shipper2';

UPDATE [users] 
SET [full_name] = N'Lê Hoàng Vũ (Quận 12)'
WHERE [username] = 'shipper3';

-- 2. Cập nhật hạng VIP và điểm tích lũy demo cho 4 khách hàng
UPDATE [users]
SET [membership_tier] = 'DIAMOND', [loyalty_points] = 1250
WHERE [username] = 'user1';

UPDATE [users]
SET [membership_tier] = 'GOLD', [loyalty_points] = 620
WHERE [username] = 'user2';

UPDATE [users]
SET [membership_tier] = 'SILVER', [loyalty_points] = 210
WHERE [username] = 'user3';

UPDATE [users]
SET [membership_tier] = 'BRONZE', [loyalty_points] = 45
WHERE [username] = 'user4';

PRINT N'=== BẮT ĐẦU CHUẨN HÓA TOÀN BỘ ĐƠN HÀNG SANG 3 QUẬN TP.HCM ===';

-- 3. Chuẩn hóa địa chỉ các đơn hàng sang Quận Tân Phú, Tân Bình, Quận 12
-- Nhóm 1: Chuyển sang Quận Tân Phú (gán cho shipper1)
UPDATE o
SET o.shipping_address = N'123 Lũy Bán Bích, Phường Hòa Thạnh, Quận Tân Phú, TP. Hồ Chí Minh',
    o.shipper_id = (SELECT id FROM [users] WHERE [username] = 'shipper1')
FROM [orders] o
WHERE (o.shipping_address NOT LIKE N'%Tân Phú%' 
   AND o.shipping_address NOT LIKE N'%Tân Bình%' 
   AND o.shipping_address NOT LIKE N'%Quận 12%')
  AND (o.id % 3 = 0)
  AND o.status <> 'HUY';

-- Nhóm 2: Chuyển sang Quận Tân Bình (gán cho shipper2)
UPDATE o
SET o.shipping_address = N'45 Cộng Hòa, Phường 13, Quận Tân Bình, TP. Hồ Chí Minh',
    o.shipper_id = (SELECT id FROM [users] WHERE [username] = 'shipper2')
FROM [orders] o
WHERE (o.shipping_address NOT LIKE N'%Tân Phú%' 
   AND o.shipping_address NOT LIKE N'%Tân Bình%' 
   AND o.shipping_address NOT LIKE N'%Quận 12%')
  AND (o.id % 3 = 1)
  AND o.status <> 'HUY';

-- Nhóm 3: Chuyển sang Quận 12 (gán cho shipper3)
UPDATE o
SET o.shipping_address = N'78 Lê Văn Khương, Phường Thới An, Quận 12, TP. Hồ Chí Minh',
    o.shipper_id = (SELECT id FROM [users] WHERE [username] = 'shipper3')
FROM [orders] o
WHERE (o.shipping_address NOT LIKE N'%Tân Phú%' 
   AND o.shipping_address NOT LIKE N'%Tân Bình%' 
   AND o.shipping_address NOT LIKE N'%Quận 12%')
  AND (o.id % 3 = 2)
  AND o.status <> 'HUY';

-- 4. Đồng bộ shipper_id đối với tất cả đơn hàng đã có địa chỉ chuẩn 3 quận
UPDATE o
SET o.shipper_id = (SELECT id FROM [users] WHERE [username] = 'shipper1')
FROM [orders] o
WHERE o.shipping_address LIKE N'%Tân Phú%' AND o.status <> 'HUY';

UPDATE o
SET o.shipper_id = (SELECT id FROM [users] WHERE [username] = 'shipper2')
FROM [orders] o
WHERE o.shipping_address LIKE N'%Tân Bình%' AND o.status <> 'HUY';

UPDATE o
SET o.shipper_id = (SELECT id FROM [users] WHERE [username] = 'shipper3')
FROM [orders] o
WHERE o.shipping_address LIKE N'%Quận 12%' AND o.status <> 'HUY';

PRINT N'=== HOÀN TẤT ĐỒNG BỘ 100% ĐƠN HÀNG VÀ PHÂN BỔ THEO 3 QUẬN TP.HCM ===';
GO

-- =============================================================
-- supermarket_db1_JAVA_100_PERCENT_FIXED.sql
-- SQL Server database aligned to current Java/JPA entities in
-- Mini_mart-main (Spring Boot 3.2 + Hibernate/JPA).
-- Target DB: supermarket_db1
-- IMPORTANT: This script rebuilds all application tables.
-- =============================================================

IF DB_ID(N'supermarket_db1') IS NULL
BEGIN
    CREATE DATABASE supermarket_db1;
END
GO

USE supermarket_db1;
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

-- =============================================================
-- 0. DROP OLD TABLES IN FK-SAFE ORDER
-- =============================================================
IF OBJECT_ID(N'dbo.payments', N'U') IS NOT NULL DROP TABLE dbo.payments;
IF OBJECT_ID(N'dbo.payment_method_configs', N'U') IS NOT NULL DROP TABLE dbo.payment_method_configs;
IF OBJECT_ID(N'dbo.order_items', N'U') IS NOT NULL DROP TABLE dbo.order_items;
IF OBJECT_ID(N'dbo.orders', N'U') IS NOT NULL DROP TABLE dbo.orders;
IF OBJECT_ID(N'dbo.reviews', N'U') IS NOT NULL DROP TABLE dbo.reviews;
IF OBJECT_ID(N'dbo.cart_items', N'U') IS NOT NULL DROP TABLE dbo.cart_items;
IF OBJECT_ID(N'dbo.cart', N'U') IS NOT NULL DROP TABLE dbo.cart;
IF OBJECT_ID(N'dbo.inventory_ledger', N'U') IS NOT NULL DROP TABLE dbo.inventory_ledger;
IF OBJECT_ID(N'dbo.product_batches', N'U') IS NOT NULL DROP TABLE dbo.product_batches;
IF OBJECT_ID(N'dbo.goods_receipt_items', N'U') IS NOT NULL DROP TABLE dbo.goods_receipt_items;
IF OBJECT_ID(N'dbo.goods_receipt', N'U') IS NOT NULL DROP TABLE dbo.goods_receipt;
IF OBJECT_ID(N'dbo.inventory', N'U') IS NOT NULL DROP TABLE dbo.inventory;
IF OBJECT_ID(N'dbo.product_images', N'U') IS NOT NULL DROP TABLE dbo.product_images;
IF OBJECT_ID(N'dbo.addresses', N'U') IS NOT NULL DROP TABLE dbo.addresses;
IF OBJECT_ID(N'dbo.coupons', N'U') IS NOT NULL DROP TABLE dbo.coupons;
IF OBJECT_ID(N'dbo.products', N'U') IS NOT NULL DROP TABLE dbo.products;
IF OBJECT_ID(N'dbo.suppliers', N'U') IS NOT NULL DROP TABLE dbo.suppliers;
IF OBJECT_ID(N'dbo.users', N'U') IS NOT NULL DROP TABLE dbo.users;
IF OBJECT_ID(N'dbo.brands', N'U') IS NOT NULL DROP TABLE dbo.brands;
IF OBJECT_ID(N'dbo.categories', N'U') IS NOT NULL DROP TABLE dbo.categories;
IF OBJECT_ID(N'dbo.roles', N'U') IS NOT NULL DROP TABLE dbo.roles;
GO

-- =============================================================
-- 1. ROLES
-- Java: Role.java -> roles(id, name)
-- =============================================================
CREATE TABLE dbo.roles (
    id   BIGINT IDENTITY(1,1) NOT NULL,
    name NVARCHAR(50) NOT NULL,
    CONSTRAINT PK_roles PRIMARY KEY (id),
    CONSTRAINT UQ_roles_name UNIQUE (name)
);
GO

-- =============================================================
-- 2. USERS
-- Java: User.java
-- password is nullable because OAuth2 users can have no local password.
-- =============================================================
CREATE TABLE dbo.users (
    id              BIGINT IDENTITY(1,1) NOT NULL,
    username        NVARCHAR(50) NOT NULL,
    email           NVARCHAR(100) NOT NULL,
    password        NVARCHAR(255) NULL,
    full_name       NVARCHAR(100) NULL,
    phone           NVARCHAR(20) NULL,
    role_id         BIGINT NULL,
    is_active       BIT NOT NULL CONSTRAINT DF_users_is_active DEFAULT (1),
    loyalty_points  INT NOT NULL CONSTRAINT DF_users_loyalty_points DEFAULT (0),
    membership_tier NVARCHAR(50) NOT NULL CONSTRAINT DF_users_membership_tier DEFAULT (N'BRONZE'),
    created_at      DATETIME2 NOT NULL CONSTRAINT DF_users_created_at DEFAULT (GETDATE()),
    provider        NVARCHAR(20) NULL CONSTRAINT DF_users_provider DEFAULT (N'LOCAL'),
    provider_id     NVARCHAR(100) NULL,
    avatar_url      NVARCHAR(512) NULL,
    CONSTRAINT PK_users PRIMARY KEY (id),
    CONSTRAINT UQ_users_username UNIQUE (username),
    CONSTRAINT UQ_users_email UNIQUE (email),
    CONSTRAINT FK_users_role FOREIGN KEY (role_id) REFERENCES dbo.roles(id) ON DELETE SET NULL
);
GO

-- =============================================================
-- 3. ADDRESSES
-- Java: Address.java
-- =============================================================
CREATE TABLE dbo.addresses (
    id             BIGINT IDENTITY(1,1) NOT NULL,
    user_id        BIGINT NULL,
    receiver_name  NVARCHAR(100) NOT NULL,
    receiver_phone NVARCHAR(20) NOT NULL,
    province       NVARCHAR(100) NOT NULL,
    district       NVARCHAR(100) NOT NULL,
    ward           NVARCHAR(100) NOT NULL,
    detail_address NVARCHAR(255) NOT NULL,
    is_default     BIT NOT NULL CONSTRAINT DF_addresses_is_default DEFAULT (0),
    CONSTRAINT PK_addresses PRIMARY KEY (id),
    CONSTRAINT FK_addresses_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE SET NULL
);
GO

-- =============================================================
-- 4. CATEGORIES
-- Java: Category.java
-- =============================================================
CREATE TABLE dbo.categories (
    id         BIGINT IDENTITY(1,1) NOT NULL,
    name       NVARCHAR(100) NOT NULL,
    description NVARCHAR(500) NULL,
    image      NVARCHAR(255) NULL,
    is_active  BIT NOT NULL CONSTRAINT DF_categories_is_active DEFAULT (1),
    created_at DATETIME2 NOT NULL CONSTRAINT DF_categories_created_at DEFAULT (GETDATE()),
    CONSTRAINT PK_categories PRIMARY KEY (id),
    CONSTRAINT UQ_categories_name UNIQUE (name)
);
GO

-- =============================================================
-- 5. BRANDS
-- Java: Brand.java
-- =============================================================
CREATE TABLE dbo.brands (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    name        NVARCHAR(100) NOT NULL,
    description NVARCHAR(500) NULL,
    is_active   BIT NOT NULL CONSTRAINT DF_brands_is_active DEFAULT (1),
    created_at  DATETIME2 NOT NULL CONSTRAINT DF_brands_created_at DEFAULT (GETDATE()),
    CONSTRAINT PK_brands PRIMARY KEY (id),
    CONSTRAINT UQ_brands_name UNIQUE (name)
);
GO

-- =============================================================
-- 6. SUPPLIERS
-- Java: Supplier.java
-- =============================================================
CREATE TABLE dbo.suppliers (
    id           BIGINT IDENTITY(1,1) NOT NULL,
    name         NVARCHAR(255) NOT NULL,
    contact_name NVARCHAR(100) NULL,
    phone        NVARCHAR(20) NULL,
    email        NVARCHAR(100) NULL,
    address      NVARCHAR(500) NULL,
    is_active    BIT NOT NULL CONSTRAINT DF_suppliers_is_active DEFAULT (1),
    created_at   DATETIME2 NOT NULL CONSTRAINT DF_suppliers_created_at DEFAULT (GETDATE()),
    CONSTRAINT PK_suppliers PRIMARY KEY (id)
);
GO

-- =============================================================
-- 7. PRODUCTS
-- Java: Product.java
-- Exact fields: name, description, price, sale_price, category_id,
-- brand_id, main_image, sku, barcode, unit, weight_g, is_active,
-- created_at, updated_at.
-- =============================================================
CREATE TABLE dbo.products (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    name        NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX) NULL,
    price       DECIMAL(18,2) NOT NULL,
    sale_price  DECIMAL(18,2) NULL,
    category_id BIGINT NULL,
    brand_id    BIGINT NULL,
    main_image  NVARCHAR(255) NULL,
    sku         NVARCHAR(50) NULL,
    barcode     NVARCHAR(100) NULL,
    unit        NVARCHAR(50) NULL,
    weight_g    INT NULL,
    is_active   BIT NOT NULL CONSTRAINT DF_products_is_active DEFAULT (1),
    created_at  DATETIME2 NOT NULL CONSTRAINT DF_products_created_at DEFAULT (GETDATE()),
    updated_at  DATETIME2 NULL,
    CONSTRAINT PK_products PRIMARY KEY (id),
    CONSTRAINT UQ_products_name UNIQUE (name),
    CONSTRAINT UQ_products_sku UNIQUE (sku),
    CONSTRAINT CK_products_price_nonnegative CHECK (price >= 0),
    CONSTRAINT CK_products_sale_price_nonnegative CHECK (sale_price IS NULL OR sale_price >= 0),
    CONSTRAINT FK_products_category FOREIGN KEY (category_id) REFERENCES dbo.categories(id) ON DELETE SET NULL,
    CONSTRAINT FK_products_brand FOREIGN KEY (brand_id) REFERENCES dbo.brands(id) ON DELETE SET NULL
);
GO

-- =============================================================
-- 8. PRODUCT IMAGES
-- Java: ProductImage.java
-- =============================================================
CREATE TABLE dbo.product_images (
    id         BIGINT IDENTITY(1,1) NOT NULL,
    product_id BIGINT NULL,
    image_path NVARCHAR(255) NOT NULL,
    sort_order INT NULL CONSTRAINT DF_product_images_sort_order DEFAULT (0),
    CONSTRAINT PK_product_images PRIMARY KEY (id),
    CONSTRAINT FK_product_images_product FOREIGN KEY (product_id) REFERENCES dbo.products(id) ON DELETE CASCADE
);
GO

-- =============================================================
-- 9. INVENTORY
-- Java: Inventory.java
-- @OneToOne product + @Check(current_stock >= 0)
-- =============================================================
CREATE TABLE dbo.inventory (
    id            BIGINT IDENTITY(1,1) NOT NULL,
    product_id    BIGINT NULL,
    current_stock INT NOT NULL CONSTRAINT DF_inventory_current_stock DEFAULT (0),
    minimum_stock INT NOT NULL CONSTRAINT DF_inventory_minimum_stock DEFAULT (5),
    location      NVARCHAR(100) NULL,
    last_updated  DATETIME2 NULL,
    created_at    DATETIME2 NOT NULL CONSTRAINT DF_inventory_created_at DEFAULT (GETDATE()),
    CONSTRAINT PK_inventory PRIMARY KEY (id),
    CONSTRAINT UQ_inventory_product UNIQUE (product_id),
    CONSTRAINT CK_inventory_current_stock_nonnegative CHECK (current_stock >= 0),
    CONSTRAINT FK_inventory_product FOREIGN KEY (product_id) REFERENCES dbo.products(id) ON DELETE CASCADE
);
GO

-- =============================================================
-- 10. GOODS RECEIPT
-- Java: GoodsReceipt.java
-- =============================================================
CREATE TABLE dbo.goods_receipt (
    id           BIGINT IDENTITY(1,1) NOT NULL,
    supplier_id  BIGINT NULL,
    total_amount DECIMAL(18,2) NOT NULL CONSTRAINT DF_goods_receipt_total DEFAULT (0),
    note         NVARCHAR(500) NULL,
    status       NVARCHAR(50) NOT NULL CONSTRAINT DF_goods_receipt_status DEFAULT (N'DRAFT'),
    created_by   BIGINT NULL,
    created_at   DATETIME2 NOT NULL CONSTRAINT DF_goods_receipt_created_at DEFAULT (GETDATE()),
    updated_at   DATETIME2 NULL,
    CONSTRAINT PK_goods_receipt PRIMARY KEY (id),
    CONSTRAINT CK_goods_receipt_total_nonnegative CHECK (total_amount >= 0),
    CONSTRAINT FK_goods_receipt_supplier FOREIGN KEY (supplier_id) REFERENCES dbo.suppliers(id) ON DELETE NO ACTION,
    CONSTRAINT FK_goods_receipt_created_by FOREIGN KEY (created_by) REFERENCES dbo.users(id) ON DELETE SET NULL
);
GO

-- =============================================================
-- 11. GOODS RECEIPT ITEMS
-- Java: GoodsReceiptItem.java
-- =============================================================
CREATE TABLE dbo.goods_receipt_items (
    id               BIGINT IDENTITY(1,1) NOT NULL,
    goods_receipt_id BIGINT NOT NULL,
    product_id       BIGINT NOT NULL,
    quantity         INT NOT NULL,
    import_price     DECIMAL(18,2) NOT NULL,
    batch_name       NVARCHAR(50) NULL,
    expiry_date      DATE NULL,
    CONSTRAINT PK_goods_receipt_items PRIMARY KEY (id),
    CONSTRAINT CK_goods_receipt_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT CK_goods_receipt_items_import_price_nonnegative CHECK (import_price >= 0),
    CONSTRAINT FK_goods_receipt_items_receipt FOREIGN KEY (goods_receipt_id) REFERENCES dbo.goods_receipt(id) ON DELETE CASCADE,
    CONSTRAINT FK_goods_receipt_items_product FOREIGN KEY (product_id) REFERENCES dbo.products(id) ON DELETE NO ACTION
);
GO

-- =============================================================
-- 12. PRODUCT BATCHES
-- Java: ProductBatch.java
-- =============================================================
CREATE TABLE dbo.product_batches (
    id               BIGINT IDENTITY(1,1) NOT NULL,
    product_id       BIGINT NOT NULL,
    goods_receipt_id BIGINT NULL,
    batch_name       NVARCHAR(50) NOT NULL,
    quantity         INT NOT NULL CONSTRAINT DF_product_batches_quantity DEFAULT (0),
    expiry_date      DATE NOT NULL,
    created_at       DATETIME2 NOT NULL CONSTRAINT DF_product_batches_created_at DEFAULT (GETDATE()),
    CONSTRAINT PK_product_batches PRIMARY KEY (id),
    CONSTRAINT CK_product_batches_quantity_nonnegative CHECK (quantity >= 0),
    CONSTRAINT FK_product_batches_product FOREIGN KEY (product_id) REFERENCES dbo.products(id) ON DELETE CASCADE,
    CONSTRAINT FK_product_batches_receipt FOREIGN KEY (goods_receipt_id) REFERENCES dbo.goods_receipt(id) ON DELETE SET NULL
);
GO

-- =============================================================
-- 13. INVENTORY LEDGER
-- Java: InventoryLedger.java
-- =============================================================
CREATE TABLE dbo.inventory_ledger (
    id              BIGINT IDENTITY(1,1) NOT NULL,
    inventory_id    BIGINT NOT NULL,
    change_type     NVARCHAR(50) NOT NULL,
    quantity_change INT NOT NULL,
    previous_stock  INT NOT NULL,
    new_stock       INT NOT NULL,
    reference_id    BIGINT NULL,
    note            NVARCHAR(500) NULL,
    created_by      BIGINT NULL,
    created_at      DATETIME2 NOT NULL CONSTRAINT DF_inventory_ledger_created_at DEFAULT (GETDATE()),
    CONSTRAINT PK_inventory_ledger PRIMARY KEY (id),
    CONSTRAINT FK_inventory_ledger_inventory FOREIGN KEY (inventory_id) REFERENCES dbo.inventory(id) ON DELETE CASCADE,
    CONSTRAINT FK_inventory_ledger_created_by FOREIGN KEY (created_by) REFERENCES dbo.users(id) ON DELETE SET NULL
);
GO

-- =============================================================
-- 14. CART
-- Java: Cart.java
-- =============================================================
CREATE TABLE dbo.cart (
    id         BIGINT IDENTITY(1,1) NOT NULL,
    user_id    BIGINT NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT DF_cart_created_at DEFAULT (GETDATE()),
    CONSTRAINT PK_cart PRIMARY KEY (id),
    CONSTRAINT UQ_cart_user UNIQUE (user_id),
    CONSTRAINT FK_cart_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE SET NULL
);
GO

-- =============================================================
-- 15. CART ITEMS
-- Java: CartItem.java
-- =============================================================
CREATE TABLE dbo.cart_items (
    id         BIGINT IDENTITY(1,1) NOT NULL,
    cart_id    BIGINT NULL,
    product_id BIGINT NULL,
    quantity   INT NOT NULL CONSTRAINT DF_cart_items_quantity DEFAULT (1),
    CONSTRAINT PK_cart_items PRIMARY KEY (id),
    CONSTRAINT UQ_cart_items_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT CK_cart_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT FK_cart_items_cart FOREIGN KEY (cart_id) REFERENCES dbo.cart(id) ON DELETE CASCADE,
    CONSTRAINT FK_cart_items_product FOREIGN KEY (product_id) REFERENCES dbo.products(id) ON DELETE CASCADE
);
GO

-- =============================================================
-- 16. ORDERS
-- Java: Order.java
-- =============================================================
CREATE TABLE dbo.orders (
    id                     BIGINT IDENTITY(1,1) NOT NULL,
    user_id                BIGINT NULL,
    shipper_id             BIGINT NULL,
    total_amount           DECIMAL(18,2) NOT NULL,
    discount_amount        DECIMAL(18,2) NULL,
    final_amount           DECIMAL(18,2) NOT NULL,
    status                 NVARCHAR(30) NOT NULL CONSTRAINT DF_orders_status DEFAULT (N'CHO_XAC_NHAN'),
    shipping_name          NVARCHAR(100) NOT NULL,
    shipping_phone         NVARCHAR(20) NOT NULL,
    shipping_address       NVARCHAR(500) NOT NULL,
    payment_method         NVARCHAR(50) NOT NULL CONSTRAINT DF_orders_payment_method DEFAULT (N'COD'),
    coupon_code            NVARCHAR(50) NULL,
    note                   NVARCHAR(500) NULL,
    delivery_note          NVARCHAR(500) NULL,
    delivered_at           DATETIME2 NULL,
    delivery_failed_reason NVARCHAR(500) NULL,
    created_at             DATETIME2 NOT NULL CONSTRAINT DF_orders_created_at DEFAULT (GETDATE()),
    updated_at             DATETIME2 NULL,
    CONSTRAINT PK_orders PRIMARY KEY (id),
    CONSTRAINT CK_orders_total_nonnegative CHECK (total_amount >= 0),
    CONSTRAINT CK_orders_discount_nonnegative CHECK (discount_amount IS NULL OR discount_amount >= 0),
    CONSTRAINT CK_orders_final_nonnegative CHECK (final_amount >= 0),
    CONSTRAINT FK_orders_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE SET NULL,
    CONSTRAINT FK_orders_shipper FOREIGN KEY (shipper_id) REFERENCES dbo.users(id) ON DELETE NO ACTION
);
GO

-- =============================================================
-- 17. ORDER ITEMS
-- Java: OrderItem.java
-- =============================================================
CREATE TABLE dbo.order_items (
    id            BIGINT IDENTITY(1,1) NOT NULL,
    order_id      BIGINT NULL,
    product_id    BIGINT NULL,
    quantity      INT NOT NULL,
    price         DECIMAL(18,2) NOT NULL,
    product_name  NVARCHAR(255) NOT NULL,
    product_image NVARCHAR(255) NULL,
    CONSTRAINT PK_order_items PRIMARY KEY (id),
    CONSTRAINT CK_order_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT CK_order_items_price_nonnegative CHECK (price >= 0),
    CONSTRAINT FK_order_items_order FOREIGN KEY (order_id) REFERENCES dbo.orders(id) ON DELETE CASCADE,
    CONSTRAINT FK_order_items_product FOREIGN KEY (product_id) REFERENCES dbo.products(id) ON DELETE SET NULL
);
GO

-- =============================================================
-- 18. PAYMENTS
-- Java: Payment.java
-- =============================================================
CREATE TABLE dbo.payments (
    id              BIGINT IDENTITY(1,1) NOT NULL,
    order_id        BIGINT NULL,
    payment_method  NVARCHAR(50) NOT NULL,
    payment_status  NVARCHAR(30) NOT NULL CONSTRAINT DF_payments_status DEFAULT (N'PENDING'),
    transaction_id  NVARCHAR(100) NULL,
    amount          DECIMAL(18,2) NOT NULL,
    paid_at         DATETIME2 NULL,
    CONSTRAINT PK_payments PRIMARY KEY (id),
    CONSTRAINT UQ_payments_order UNIQUE (order_id),
    CONSTRAINT CK_payments_amount_nonnegative CHECK (amount >= 0),
    CONSTRAINT FK_payments_order FOREIGN KEY (order_id) REFERENCES dbo.orders(id) ON DELETE CASCADE
);
GO

-- =============================================================
-- 19. REVIEWS
-- Java: Review.java
-- =============================================================
CREATE TABLE dbo.reviews (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    product_id  BIGINT NULL,
    user_id     BIGINT NULL,
    rating      INT NOT NULL,
    comment     NVARCHAR(1000) NULL,
    is_approved BIT NOT NULL CONSTRAINT DF_reviews_is_approved DEFAULT (0),
    created_at  DATETIME2 NOT NULL CONSTRAINT DF_reviews_created_at DEFAULT (GETDATE()),
    CONSTRAINT PK_reviews PRIMARY KEY (id),
    CONSTRAINT CK_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT FK_reviews_product FOREIGN KEY (product_id) REFERENCES dbo.products(id) ON DELETE CASCADE,
    CONSTRAINT FK_reviews_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE SET NULL
);
GO

-- =============================================================
-- 20. COUPONS
-- Java: Coupon.java
-- =============================================================
CREATE TABLE dbo.coupons (
    id               BIGINT IDENTITY(1,1) NOT NULL,
    code             NVARCHAR(50) NOT NULL,
    description      NVARCHAR(255) NULL,
    discount_type    NVARCHAR(20) NOT NULL,
    discount_value   DECIMAL(18,2) NOT NULL,
    start_date       DATETIME2 NOT NULL,
    end_date         DATETIME2 NOT NULL,
    min_order_amount DECIMAL(18,2) NULL,
    used_count       INT NOT NULL CONSTRAINT DF_coupons_used_count DEFAULT (0),
    max_uses         INT NULL CONSTRAINT DF_coupons_max_uses DEFAULT (100),
    is_active        BIT NOT NULL CONSTRAINT DF_coupons_is_active DEFAULT (1),
    created_at       DATETIME2 NOT NULL CONSTRAINT DF_coupons_created_at DEFAULT (GETDATE()),
    CONSTRAINT PK_coupons PRIMARY KEY (id),
    CONSTRAINT UQ_coupons_code UNIQUE (code),
    CONSTRAINT CK_coupons_discount_value_positive CHECK (discount_value > 0),
    CONSTRAINT CK_coupons_min_order_nonnegative CHECK (min_order_amount IS NULL OR min_order_amount >= 0),
    CONSTRAINT CK_coupons_used_count_nonnegative CHECK (used_count >= 0),
    CONSTRAINT CK_coupons_max_uses_nonnegative CHECK (max_uses IS NULL OR max_uses >= 0)
);
GO

-- =============================================================
-- 21. PAYMENT METHOD CONFIGS
-- Java: PaymentMethodConfig.java -> payment_method_configs
-- =============================================================
CREATE TABLE dbo.payment_method_configs (
    id              BIGINT IDENTITY(1,1) NOT NULL,
    method_key      NVARCHAR(50) NOT NULL,
    name            NVARCHAR(100) NOT NULL,
    description     NVARCHAR(500) NULL,
    is_enabled      BIT NOT NULL CONSTRAINT DF_payment_method_configs_is_enabled DEFAULT (1),
    account_number  NVARCHAR(50) NULL,
    account_holder  NVARCHAR(100) NULL,
    bank_name       NVARCHAR(100) NULL,
    qr_code_url     NVARCHAR(500) NULL,
    transfer_syntax NVARCHAR(200) NULL,
    created_at      DATETIME2(7) NOT NULL CONSTRAINT DF_payment_method_configs_created_at DEFAULT (SYSDATETIME()),
    updated_at      DATETIME2(7) NULL,
    CONSTRAINT PK_payment_method_configs PRIMARY KEY (id),
    CONSTRAINT UQ_payment_method_configs_key UNIQUE (method_key)
);
GO

-- =============================================================
-- 21. INDEXES USED BY COMMON REPOSITORY QUERIES
-- =============================================================
CREATE INDEX IX_users_role_id ON dbo.users(role_id);
CREATE INDEX IX_addresses_user_id ON dbo.addresses(user_id);
CREATE INDEX IX_products_category_id ON dbo.products(category_id);
CREATE INDEX IX_products_brand_id ON dbo.products(brand_id);
CREATE INDEX IX_product_images_product_id ON dbo.product_images(product_id);
CREATE INDEX IX_goods_receipt_supplier_id ON dbo.goods_receipt(supplier_id);
CREATE INDEX IX_goods_receipt_created_by ON dbo.goods_receipt(created_by);
CREATE INDEX IX_goods_receipt_items_receipt_id ON dbo.goods_receipt_items(goods_receipt_id);
CREATE INDEX IX_goods_receipt_items_product_id ON dbo.goods_receipt_items(product_id);
CREATE INDEX IX_product_batches_product_id_expiry ON dbo.product_batches(product_id, expiry_date);
CREATE INDEX IX_product_batches_receipt_id ON dbo.product_batches(goods_receipt_id);
CREATE INDEX IX_inventory_ledger_inventory_created_at ON dbo.inventory_ledger(inventory_id, created_at DESC);
CREATE INDEX IX_inventory_ledger_created_by ON dbo.inventory_ledger(created_by);
CREATE INDEX IX_orders_user_created_at ON dbo.orders(user_id, created_at DESC);
CREATE INDEX IX_orders_shipper_id ON dbo.orders(shipper_id);
CREATE INDEX IX_order_items_order_id ON dbo.order_items(order_id);
CREATE INDEX IX_order_items_product_id ON dbo.order_items(product_id);
CREATE INDEX IX_reviews_product_approved_created ON dbo.reviews(product_id, is_approved, created_at DESC);
CREATE INDEX IX_reviews_user_id ON dbo.reviews(user_id);
GO

-- =============================================================
-- 22. SEED DATA
-- These rows intentionally match the current DataInitializer's
-- categories/brands/products/inventory/coupons and use plain
-- 'pass1234' because current SecurityConfig uses NoOpPasswordEncoder.
-- =============================================================

-- Roles
SET IDENTITY_INSERT dbo.roles ON;
INSERT INTO dbo.roles (id, name) VALUES
(1, N'ROLE_ADMIN'),
(2, N'ROLE_USER'),
(3, N'ROLE_SHIPPER');
SET IDENTITY_INSERT dbo.roles OFF;

-- Users
SET IDENTITY_INSERT dbo.users ON;
INSERT INTO dbo.users
(id, username, email, password, full_name, phone, role_id, is_active, loyalty_points, membership_tier, provider, provider_id, avatar_url, created_at)
VALUES
(1, N'admin',    N'admin@supermarket.com', N'pass1234', N'Quản Trị Viên',             N'0912345678', 1, 1, 0, N'BRONZE', N'LOCAL', NULL, NULL, GETDATE()),
(2, N'user1',    N'user1@gmail.com',       N'pass1234', N'Nguyễn Văn A',              N'0987654321', 2, 1, 120, N'SILVER', N'LOCAL', NULL, NULL, GETDATE()),
(3, N'user2',    N'user2@gmail.com',       N'pass1234', N'Trần Thị B',                N'0977888999', 2, 1, 550, N'GOLD', N'LOCAL', NULL, NULL, GETDATE()),
(4, N'shipper1', N'shipper1@supermarket.com', N'pass1234', N'Nguyễn Văn Giao (Tân Phú)', N'0901234567', 3, 1, 0, N'BRONZE', N'LOCAL', NULL, NULL, GETDATE()),
(5, N'shipper2', N'shipper2@supermarket.com', N'pass1234', N'Trần Văn Tốc (Tân Bình)', N'0907654321', 3, 1, 0, N'BRONZE', N'LOCAL', NULL, NULL, GETDATE()),
(6, N'shipper3', N'shipper3@supermarket.com', N'pass1234', N'Lê Hoàng Vận (Quận 12)',  N'0903334455', 3, 1, 0, N'BRONZE', N'LOCAL', NULL, NULL, GETDATE());
SET IDENTITY_INSERT dbo.users OFF;

-- Addresses
SET IDENTITY_INSERT dbo.addresses ON;
INSERT INTO dbo.addresses
(id, user_id, receiver_name, receiver_phone, province, district, ward, detail_address, is_default)
VALUES
(1, 2, N'Nguyễn Văn A', N'0987654321', N'TP Hồ Chí Minh', N'Tân Phú', N'Tây Thạnh', N'Số 120 Tây Thạnh', 1),
(2, 3, N'Trần Thị B', N'0977888999', N'TP Hồ Chí Minh', N'Tân Bình', N'Phường 2', N'45 Bạch Đằng', 1),
(3, 2, N'Nguyễn Văn A', N'0987654321', N'TP Hồ Chí Minh', N'Quận 12', N'Tân Thới Hiệp', N'88 Dương Thị Mười', 0);
SET IDENTITY_INSERT dbo.addresses OFF;

-- Categories: exactly the five categories expected by DataInitializer
SET IDENTITY_INSERT dbo.categories ON;
INSERT INTO dbo.categories (id, name, description, image, is_active, created_at) VALUES
(1, N'Rau Củ Quả', N'Rau củ tươi ngon VietGAP', N'https://images.unsplash.com/photo-1566385101042-1a0aa0c1268c?q=80&w=300&auto=format&fit=crop', 1, GETDATE()),
(2, N'Thực Phẩm Khô', N'Mì ăn liền, gạo, gia vị', N'https://images.unsplash.com/photo-1586201375761-83865001e31c?q=80&w=300&auto=format&fit=crop', 1, GETDATE()),
(3, N'Sữa & Bơ sữa', N'Sữa tươi tiệt trùng, bơ, sữa chua', N'https://images.unsplash.com/photo-1628088062854-d1870b4553da?q=80&w=300&auto=format&fit=crop', 1, GETDATE()),
(4, N'Đồ Uống', N'Nước ngọt, bia, nước trái cây', N'https://images.unsplash.com/photo-1622483767028-3f66f32aef97?q=80&w=300&auto=format&fit=crop', 1, GETDATE()),
(5, N'Hóa Mỹ Phẩm', N'Dầu gội, bột giặt, nước rửa chén', N'https://images.unsplash.com/photo-1584824486509-112e4181ff6b?q=80&w=300&auto=format&fit=crop', 1, GETDATE());
SET IDENTITY_INSERT dbo.categories OFF;

-- Brands: exactly the five brands expected by DataInitializer
SET IDENTITY_INSERT dbo.brands ON;
INSERT INTO dbo.brands (id, name, description, is_active, created_at) VALUES
(1, N'VietGAP Farm', N'Rau sạch tiêu chuẩn VietGAP', 1, GETDATE()),
(2, N'Acecook', N'Mì gói ăn liền Hảo Hảo', 1, GETDATE()),
(3, N'Vinamilk', N'Sữa tươi, sữa chua quốc dân', 1, GETDATE()),
(4, N'Coca Cola', N'Thương hiệu giải khát', 1, GETDATE()),
(5, N'Unilever', N'Clear, Sunlight, Omo', 1, GETDATE());
SET IDENTITY_INSERT dbo.brands OFF;

-- Products: exactly the ten products created by DataInitializer
SET IDENTITY_INSERT dbo.products ON;
INSERT INTO dbo.products
(id, name, description, price, sale_price, category_id, brand_id, main_image, sku, barcode, unit, weight_g, is_active, created_at, updated_at)
VALUES
(1, N'Xà lách thủy canh sạch', N'Xà lách tươi ngon được trồng theo phương pháp thủy canh, sạch sẽ an toàn.', 25000.00, 20000.00, 1, 1, N'/uploads/1.jpg',  N'SP001', N'8931234567901', N'Kg',   1000, 1, GETDATE(), NULL),
(2, N'Cà chua VietGAP 1kg', N'Cà chua đỏ chín tự nhiên, nhiều dinh dưỡng, không hóa chất bảo quản.', 35000.00, 30000.00, 1, 1, N'/uploads/3.jpg',  N'SP002', N'8931234567902', N'Kg',   1000, 1, GETDATE(), NULL),
(3, N'Mì Hảo Hảo Tôm Chua Cay', N'Mì ăn liền Hảo Hảo hương vị tôm chua cay truyền thống, thùng 30 gói.', 135000.00, 128000.00, 2, 2, N'/uploads/4.webp', N'SP003', N'8931234567903', N'Thùng', 3000, 1, GETDATE(), NULL),
(4, N'Dầu ăn Simply Đậu Nành 1L', N'Dầu ănSimply 100% nguyên chất từ hạt đậu nành chọn lọc, tốt cho tim mạch.', 62000.00, NULL, 2, 5, N'/uploads/5.jpg', N'SP004', N'8931234567904', N'Chai',  1000, 1, GETDATE(), NULL),
(5, N'Sữa tươi Vinamilk ít đường 1L', N'Sữa tươi tiệt trùng Vinamilk bổ sung vitamin AD3 giúp xương chắc khỏe.', 38000.00, 36000.00, 3, 3, N'/uploads/6.jpg', N'SP005', N'8931234567905', N'Hộp',  1000, 1, GETDATE(), NULL),
(6, N'Sữa chua Vinamilk có đường hộp 100g', N'Sữa chua ăn Vinamilk thơm ngon tự nhiên, hỗ trợ tiêu hóa tốt.', 8000.00, NULL, 3, 3, N'/uploads/7.jpg', N'SP006', N'8931234567906', N'Hộp',   100, 1, GETDATE(), NULL),
(7, N'Nước ngọt Coca Cola lon 320ml', N'Nước giải khát có ga Coca Cola sảng khoái cực độ.', 11000.00, 10000.00, 4, 4, N'/uploads/8.jpg', N'SP007', N'8931234567907', N'Lon',    320, 1, GETDATE(), NULL),
(8, N'Bia Heineken lon 330ml', N'Bia Heineken Premium chất lượng thượng hạng từ Hà Lan.', 22000.00, 21000.00, 4, 4, N'/uploads/9.jpg', N'SP008', N'8931234567908', N'Lon',    330, 1, GETDATE(), NULL),
(9, N'Dầu gội Clear Bạc Hà Mát Lạnh 630ml', N'Dầu gội sạch gàu số 1 Việt Nam với tinh chất bạc hà.', 175000.00, 160000.00, 5, 5, N'/uploads/10.webp', N'SP009', N'8931234567909', N'Chai',   630, 1, GETDATE(), NULL),
(10, N'Nước lau sàn Sunlight Hoa Lilia 1kg', N'Nước lau sàn Sunlight hương hoa Lilia thơm ngát, sạch bóng.', 32000.00, NULL, 5, 5, N'/uploads/11.jpg', N'SP010', N'8931234567910', N'Chai',  1000, 1, GETDATE(), NULL);
SET IDENTITY_INSERT dbo.products OFF;

-- Inventory: exactly aligned to DataInitializer
SET IDENTITY_INSERT dbo.inventory ON;
INSERT INTO dbo.inventory
(id, product_id, current_stock, minimum_stock, location, last_updated, created_at)
VALUES
(1,  1,  4, 5, N'Kệ A1 - Rau củ', GETDATE(), GETDATE()),
(2,  2, 25, 5, N'Kệ A2 - Rau củ', GETDATE(), GETDATE()),
(3,  3,  3, 5, N'Kệ B1 - Hàng khô', GETDATE(), GETDATE()),
(4,  4, 50, 5, N'Kệ B2 - Hàng khô', GETDATE(), GETDATE()),
(5,  5, 12, 5, N'Tủ lạnh 1 - Sữa', GETDATE(), GETDATE()),
(6,  6,100, 5, N'Tủ lạnh 1 - Sữa', GETDATE(), GETDATE()),
(7,  7,120, 5, N'Kệ C1 - Đồ uống', GETDATE(), GETDATE()),
(8,  8, 60, 5, N'Kệ C2 - Đồ uống', GETDATE(), GETDATE()),
(9,  9,  2, 5, N'Kệ D1 - Hóa phẩm', GETDATE(), GETDATE()),
(10,10, 30, 5, N'Kệ D2 - Hóa phẩm', GETDATE(), GETDATE());
SET IDENTITY_INSERT dbo.inventory OFF;

-- Product images
INSERT INTO dbo.product_images (product_id, image_path, sort_order) VALUES
(1, N'/uploads/1.jpg', 0),
(2, N'/uploads/3.jpg', 0),
(3, N'/uploads/4.webp', 0),
(4, N'/uploads/5.jpg', 0),
(5, N'/uploads/6.jpg', 0),
(6, N'/uploads/7.jpg', 0),
(7, N'/uploads/8.jpg', 0),
(8, N'/uploads/9.jpg', 0),
(9, N'/uploads/10.webp', 0),
(10, N'/uploads/11.jpg', 0);

-- Coupons: exactly compatible with Coupon.java and DataInitializer
SET IDENTITY_INSERT dbo.coupons ON;
INSERT INTO dbo.coupons
(id, code, description, discount_type, discount_value, start_date, end_date, min_order_amount, used_count, max_uses, is_active, created_at)
VALUES
(1, N'WELCOME10', N'Giảm 10% cho đơn hàng đầu tiên', N'PERCENTAGE', 10.00, DATEADD(DAY,-1,GETDATE()), DATEADD(YEAR,1,GETDATE()), 50000.00, 0, 1000, 1, GETDATE()),
(2, N'SIEUCAOCAP', N'Giảm 50.000đ cho đơn từ 200.000đ', N'FIXED_AMOUNT', 50000.00, DATEADD(DAY,-1,GETDATE()), DATEADD(YEAR,1,GETDATE()), 200000.00, 0, 200, 1, GETDATE()),
(3, N'FREESHIP', N'Giảm 15.000đ cho mọi đơn hàng', N'FIXED_AMOUNT', 15000.00, DATEADD(DAY,-1,GETDATE()), DATEADD(YEAR,1,GETDATE()), 0.00, 0, 500, 1, GETDATE());
SET IDENTITY_INSERT dbo.coupons OFF;

-- Reviews
INSERT INTO dbo.reviews (product_id, user_id, rating, comment, is_approved, created_at) VALUES
(1, 2, 5, N'Rau rất tươi xanh, đóng gói sạch sẽ.', 1, GETDATE()),
(1, 3, 4, N'Giao hàng nhanh, rau tươi ngon.', 1, GETDATE()),
(3, 2, 5, N'Mì Hảo Hảo ăn ngon, date xa.', 1, GETDATE());

-- =============================================================
-- 23. OPTIONAL DEMO DATA FOR GOODS RECEIPTS / BATCHES / LEDGER
-- =============================================================
INSERT INTO dbo.suppliers (name, contact_name, phone, email, address, is_active) VALUES
(N'Công ty TNHH Nông Sản Xanh', N'Nguyễn Minh Khoa', N'0909000001', N'nongsanxanh@example.com', N'TP. Hồ Chí Minh', 1),
(N'Công ty TNHH Phân Phối FMCG Việt', N'Trần Quốc Huy', N'0909000002', N'fmcg@example.com', N'Bình Dương', 1);

INSERT INTO dbo.goods_receipt (supplier_id, total_amount, note, status, created_by) VALUES
(1, 2800000.00, N'Nhập rau củ và sữa', N'COMPLETED', 1),
(2, 3440000.00, N'Nhập hàng khô và đồ uống', N'COMPLETED', 1);

INSERT INTO dbo.goods_receipt_items (goods_receipt_id, product_id, quantity, import_price, batch_name, expiry_date) VALUES
(1, 1, 100, 15000.00, N'LH2026A', DATEADD(DAY,30,CAST(GETDATE() AS DATE))),
(1, 2,  80, 20000.00, N'CT2026A', DATEADD(DAY,30,CAST(GETDATE() AS DATE))),
(1, 5,  50, 28000.00, N'ST2026A', DATEADD(DAY,180,CAST(GETDATE() AS DATE))),
(2, 3,  20, 110000.00, N'MH2026A', DATEADD(DAY,300,CAST(GETDATE() AS DATE))),
(2, 4,  30, 50000.00, N'DA2026A', DATEADD(DAY,365,CAST(GETDATE() AS DATE))),
(2, 7,  50, 8000.00, N'CC2026A', DATEADD(DAY,180,CAST(GETDATE() AS DATE)));

INSERT INTO dbo.product_batches (product_id, goods_receipt_id, batch_name, quantity, expiry_date) VALUES
(1, 1, N'LH2026A', 100, DATEADD(DAY,30,CAST(GETDATE() AS DATE))),
(2, 1, N'CT2026A',  80, DATEADD(DAY,30,CAST(GETDATE() AS DATE))),
(5, 1, N'ST2026A',  50, DATEADD(DAY,180,CAST(GETDATE() AS DATE))),
(3, 2, N'MH2026A',  20, DATEADD(DAY,300,CAST(GETDATE() AS DATE))),
(4, 2, N'DA2026A',  30, DATEADD(DAY,365,CAST(GETDATE() AS DATE))),
(7, 2, N'CC2026A',  50, DATEADD(DAY,180,CAST(GETDATE() AS DATE)));

INSERT INTO dbo.inventory_ledger
(inventory_id, change_type, quantity_change, previous_stock, new_stock, reference_id, note, created_by)
VALUES
(1, N'IMPORT', 100, 0, 100, 1, N'Nhập lô LH2026A', 1),
(1, N'SELL',   -96, 100, 4, NULL, N'Đồng bộ tồn kho mẫu', 1),
(2, N'IMPORT',  80, 0, 80, 1, N'Nhập lô CT2026A', 1),
(2, N'MANUAL_ADJUST', -55, 80, 25, NULL, N'Đồng bộ tồn kho mẫu', 1),
(3, N'IMPORT',  20, 0, 20, 2, N'Nhập lô MH2026A', 1),
(3, N'MANUAL_ADJUST', -17, 20, 3, NULL, N'Đồng bộ tồn kho mẫu', 1);

-- =============================================================
-- 24. DEMO CART
-- =============================================================
INSERT INTO dbo.cart (user_id) VALUES (2), (3);
INSERT INTO dbo.cart_items (cart_id, product_id, quantity) VALUES
(1, 5, 2),
(1, 7, 3),
(2, 1, 1);

-- =============================================================
-- 25. DEMO ORDERS / ORDER ITEMS / PAYMENTS
-- Totals are mathematically consistent with order item lines.
-- =============================================================
INSERT INTO dbo.orders
(user_id, shipper_id, total_amount, discount_amount, final_amount, status, shipping_name, shipping_phone, shipping_address, payment_method, coupon_code, note, delivery_note, delivered_at)
VALUES
(2, 4,    214000.00, 0.00,     214000.00, N'HOAN_THANH',  N'Nguyễn Văn A', N'0987654321', N'Số 120 Tây Thạnh, Phường Tây Thạnh, Quận Tân Phú, TP Hồ Chí Minh', N'COD',  NULL, N'Đơn mẫu 1', N'Đã giao tận tay', DATEADD(HOUR,-2,GETDATE())),
(3, NULL, 202000.00, 0.00,     202000.00, N'DA_XAC_NHAN', N'Trần Thị B',   N'0977888999', N'45 Bạch Đằng, Phường 2, Quận Tân Bình, TP Hồ Chí Minh',            N'BANK_TRANSFER', NULL, N'Đơn mẫu 2 Tân Bình chờ Shipper', NULL, NULL),
(2, NULL, 280000.00, 0.00,     280000.00, N'DA_XAC_NHAN', N'Nguyễn Văn A', N'0987654321', N'88 Dương Thị Mười, Phường Tân Thới Hiệp, Quận 12, TP Hồ Chí Minh', N'COD',  NULL, N'Đơn mẫu 3 Quận 12 chờ Shipper', NULL, NULL),
(3, 4,    157000.00, 30000.00, 127000.00, N'HOAN_THANH',  N'Trần Thị B',   N'0977888999', N'15 Lê Trọng Tấn, Phường Sơn Kỳ, Quận Tân Phú, TP Hồ Chí Minh',    N'MOMO', N'WELCOME10', N'Đơn mẫu có giảm giá', N'Giao thành công', DATEADD(DAY,-1,GETDATE()));

INSERT INTO dbo.order_items (order_id, product_id, quantity, price, product_name, product_image) VALUES
(1, 7, 5, 10000.00,  N'Nước ngọt Coca Cola lon 320ml', N'/uploads/8.jpg'),
(1, 5, 1, 36000.00,  N'Sữa tươi Vinamilk ít đường 1L', N'/uploads/6.jpg'),
(1, 3, 1, 128000.00, N'Mì Hảo Hảo Tôm Chua Cay', N'/uploads/4.webp'),
(2, 3, 1, 160000.00, N'Mì Hảo Hảo Tôm Chua Cay', N'/uploads/4.webp'),
(2, 5, 1, 42000.00,  N'Sữa tươi Vinamilk ít đường 1L', N'/uploads/6.jpg'),
(3, 3, 4, 39000.00, N'Mì Hảo Hảo Tôm Chua Cay', N'/uploads/4.webp'),
(3, 4, 2, 62000.00, N'Dầu ăn Simply Đậu Nành 1L', N'/uploads/5.jpg'),
(4, 5, 1, 130000.00, N'Sữa tươi Vinamilk ít đường 1L', N'/uploads/6.jpg'),
(4, 7, 3, 9000.00,   N'Nước ngọt Coca Cola lon 320ml', N'/uploads/8.jpg');

INSERT INTO dbo.payments (order_id, payment_method, payment_status, transaction_id, amount, paid_at) VALUES
(1, N'COD',            N'COMPLETED', N'COD-0001', 214000.00, GETDATE()),
(2, N'BANK_TRANSFER',  N'COMPLETED', N'BANK-0002', 202000.00, GETDATE()),
(3, N'COD',            N'PENDING',   N'COD-0003', 280000.00, NULL),
(4, N'MOMO',           N'COMPLETED', N'MOMO-0004', 127000.00, GETDATE());

-- Payment Method Configs Seed
SET IDENTITY_INSERT dbo.payment_method_configs ON;
INSERT INTO dbo.payment_method_configs
(id, method_key, name, description, is_enabled, account_number, account_holder, bank_name, qr_code_url, transfer_syntax, created_at)
VALUES
(1, N'COD',           N'Thanh toán khi nhận hàng (COD)', N'Thanh toán bằng tiền mặt trực tiếp cho Shipper khi nhận hàng tận nơi.', 1, NULL, NULL, NULL, NULL, NULL, GETDATE()),
(2, N'BANK_TRANSFER', N'Chuyển khoản VietQR',           N'Quét mã VietQR chuyển khoản nhanh 24/7 qua ứng dụng ngân hàng bất kỳ.',  1, N'0336174371', N'NGUYEN TIEN TAI', N'MBBank (Ngân hàng Quân Đội)', N'https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=2|99|0336174371|NGUYEN%20TIEN%20TAI||0|0|0|MINIMART', N'MINIMART {orderId} {phone}', GETDATE()),
(3, N'MOMO',          N'Ví điện tử MoMo',               N'Quét mã QR qua ứng dụng Ví MoMo tiện lợi và nhanh chóng.',               1, N'0336174371', N'NGUYEN TIEN TAI', N'Ví điện tử MoMo',             N'https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=2|99|0336174371|NGUYEN%20TIEN%20TAI||0|0|0|MINIMART', N'MINIMART {orderId} {phone}', GETDATE());
SET IDENTITY_INSERT dbo.payment_method_configs OFF;

-- Coupon used by demo order 4 (insert it after coupon table exists)
UPDATE dbo.coupons
SET used_count = used_count + 1
WHERE code = N'WELCOME10';

-- =============================================================
-- 26. RESET IDENTITY SEEDS
-- Keeps future JPA IDENTITY inserts above the seed IDs.
-- =============================================================
DBCC CHECKIDENT ('dbo.roles', RESEED);
DBCC CHECKIDENT ('dbo.users', RESEED);
DBCC CHECKIDENT ('dbo.addresses', RESEED);
DBCC CHECKIDENT ('dbo.categories', RESEED);
DBCC CHECKIDENT ('dbo.brands', RESEED);
DBCC CHECKIDENT ('dbo.suppliers', RESEED);
DBCC CHECKIDENT ('dbo.products', RESEED);
DBCC CHECKIDENT ('dbo.product_images', RESEED);
DBCC CHECKIDENT ('dbo.inventory', RESEED);
DBCC CHECKIDENT ('dbo.goods_receipt', RESEED);
DBCC CHECKIDENT ('dbo.goods_receipt_items', RESEED);
DBCC CHECKIDENT ('dbo.product_batches', RESEED);
DBCC CHECKIDENT ('dbo.inventory_ledger', RESEED);
DBCC CHECKIDENT ('dbo.cart', RESEED);
DBCC CHECKIDENT ('dbo.cart_items', RESEED);
DBCC CHECKIDENT ('dbo.orders', RESEED);
DBCC CHECKIDENT ('dbo.order_items', RESEED);
DBCC CHECKIDENT ('dbo.payments', RESEED);
DBCC CHECKIDENT ('dbo.payment_method_configs', RESEED);
DBCC CHECKIDENT ('dbo.reviews', RESEED);
DBCC CHECKIDENT ('dbo.coupons', RESEED);
GO

-- =============================================================
-- 27. VALIDATION
-- =============================================================
PRINT N'=============================================';
PRINT N'✅ supermarket_db1 created successfully';
PRINT N'✅ Schema aligned with current Java entity mappings';
PRINT N'✅ FK delete actions chosen to avoid SQL Server cascade-path conflicts';
PRINT N'=============================================';

SELECT N'roles' AS table_name, COUNT(*) AS row_count FROM dbo.roles
UNION ALL SELECT N'users', COUNT(*) FROM dbo.users
UNION ALL SELECT N'addresses', COUNT(*) FROM dbo.addresses
UNION ALL SELECT N'categories', COUNT(*) FROM dbo.categories
UNION ALL SELECT N'brands', COUNT(*) FROM dbo.brands
UNION ALL SELECT N'suppliers', COUNT(*) FROM dbo.suppliers
UNION ALL SELECT N'products', COUNT(*) FROM dbo.products
UNION ALL SELECT N'product_images', COUNT(*) FROM dbo.product_images
UNION ALL SELECT N'inventory', COUNT(*) FROM dbo.inventory
UNION ALL SELECT N'goods_receipt', COUNT(*) FROM dbo.goods_receipt
UNION ALL SELECT N'goods_receipt_items', COUNT(*) FROM dbo.goods_receipt_items
UNION ALL SELECT N'product_batches', COUNT(*) FROM dbo.product_batches
UNION ALL SELECT N'inventory_ledger', COUNT(*) FROM dbo.inventory_ledger
UNION ALL SELECT N'cart', COUNT(*) FROM dbo.cart
UNION ALL SELECT N'cart_items', COUNT(*) FROM dbo.cart_items
UNION ALL SELECT N'orders', COUNT(*) FROM dbo.orders
UNION ALL SELECT N'order_items', COUNT(*) FROM dbo.order_items
UNION ALL SELECT N'payments', COUNT(*) FROM dbo.payments
UNION ALL SELECT N'payment_method_configs', COUNT(*) FROM dbo.payment_method_configs
UNION ALL SELECT N'reviews', COUNT(*) FROM dbo.reviews
UNION ALL SELECT N'coupons', COUNT(*) FROM dbo.coupons;

SELECT p.id, p.name, c.name AS category_name, b.name AS brand_name, i.current_stock
FROM dbo.products p
LEFT JOIN dbo.categories c ON c.id = p.category_id
LEFT JOIN dbo.brands b ON b.id = p.brand_id
LEFT JOIN dbo.inventory i ON i.product_id = p.id
ORDER BY p.id;

SELECT o.id, o.total_amount, o.discount_amount, o.final_amount,
       SUM(oi.quantity * oi.price) AS item_total
FROM dbo.orders o
LEFT JOIN dbo.order_items oi ON oi.order_id = o.id
GROUP BY o.id, o.total_amount, o.discount_amount, o.final_amount
ORDER BY o.id;
GO

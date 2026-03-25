-- WA Shop Database Schema
-- PostgreSQL 15+

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- SHOPS TABLE
-- ============================================
CREATE TABLE shops (
    id VARCHAR(50) PRIMARY KEY DEFAULT 'SHOP_' || SUBSTRING(uuid_generate_v4()::TEXT, 1, 8),
    name VARCHAR(255) NOT NULL,
    owner_phone VARCHAR(15) NOT NULL UNIQUE,
    owner_name VARCHAR(255),
    address TEXT,
    city VARCHAR(100) DEFAULT 'Vizag',
    whatsapp_number VARCHAR(15),
    is_active BOOLEAN DEFAULT TRUE,
    subscription_plan VARCHAR(50) DEFAULT 'FREE',
    subscription_expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_shops_owner_phone ON shops(owner_phone);
CREATE INDEX idx_shops_city ON shops(city);

-- ============================================
-- USERS TABLE (Customers + Shop Owners)
-- ============================================
CREATE TABLE users (
    phone VARCHAR(15) PRIMARY KEY,
    name VARCHAR(255),
    role VARCHAR(20) DEFAULT 'CUSTOMER' CHECK (role IN ('CUSTOMER', 'SHOP_OWNER', 'ADMIN')),
    shop_id VARCHAR(50) REFERENCES shops(id) ON DELETE SET NULL,
    default_address TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    last_active_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_shop_id ON users(shop_id);
CREATE INDEX idx_users_role ON users(role);

-- ============================================
-- PRODUCTS TABLE
-- ============================================
CREATE TABLE products (
    id VARCHAR(50) PRIMARY KEY DEFAULT 'P_' || SUBSTRING(uuid_generate_v4()::TEXT, 1, 8),
    shop_id VARCHAR(50) NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
    unit VARCHAR(50) DEFAULT 'piece',
    image_url TEXT,
    category VARCHAR(100),
    stock INTEGER DEFAULT -1,
    is_available BOOLEAN DEFAULT TRUE,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_shop_id ON products(shop_id);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_available ON products(shop_id, is_available);

-- ============================================
-- CART_ITEMS TABLE
-- ============================================
CREATE TABLE cart_items (
    id SERIAL PRIMARY KEY,
    user_phone VARCHAR(15) NOT NULL,
    shop_id VARCHAR(50) NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    product_id VARCHAR(50) NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    quantity DECIMAL(10,3) NOT NULL DEFAULT 1 CHECK (quantity > 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_phone, shop_id, product_id)
);

CREATE INDEX idx_cart_user_shop ON cart_items(user_phone, shop_id);

-- ============================================
-- ORDERS TABLE
-- ============================================
CREATE TABLE orders (
    id VARCHAR(50) PRIMARY KEY DEFAULT 'ORD_' || SUBSTRING(uuid_generate_v4()::TEXT, 1, 8),
    shop_id VARCHAR(50) NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    customer_phone VARCHAR(15) NOT NULL,
    customer_name VARCHAR(255),
    delivery_address TEXT,
    subtotal DECIMAL(10, 2) NOT NULL DEFAULT 0,
    delivery_charge DECIMAL(10, 2) DEFAULT 0,
    discount DECIMAL(10, 2) DEFAULT 0,
    total DECIMAL(10, 2) NOT NULL DEFAULT 0,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED')),
    payment_status VARCHAR(20) DEFAULT 'UNPAID' CHECK (payment_status IN ('UNPAID', 'PAID', 'REFUNDED', 'COD')),
    payment_method VARCHAR(20),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    accepted_at TIMESTAMP,
    delivered_at TIMESTAMP
);

CREATE INDEX idx_orders_shop_id ON orders(shop_id);
CREATE INDEX idx_orders_customer ON orders(customer_phone);
CREATE INDEX idx_orders_status ON orders(shop_id, status);
CREATE INDEX idx_orders_date ON orders(shop_id, created_at DESC);

-- ============================================
-- ORDER_ITEMS TABLE
-- ============================================
CREATE TABLE order_items (
    id SERIAL PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id VARCHAR(50) REFERENCES products(id) ON DELETE SET NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity DECIMAL(10,3) NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10, 2) NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_order_items_order ON order_items(order_id);

-- ============================================
-- PAYMENTS TABLE
-- ============================================
CREATE TABLE payments (
    id VARCHAR(50) PRIMARY KEY DEFAULT 'PAY_' || SUBSTRING(uuid_generate_v4()::TEXT, 1, 8),
    order_id VARCHAR(50) NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'INR',
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED')),
    gateway VARCHAR(50) DEFAULT 'RAZORPAY',
    gateway_payment_id VARCHAR(255),
    gateway_order_id VARCHAR(255),
    payment_link TEXT,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payments_order ON payments(order_id);
CREATE INDEX idx_payments_gateway ON payments(gateway_payment_id);

-- ============================================
-- OTP TABLE (for authentication)
-- ============================================
CREATE TABLE otps (
    id SERIAL PRIMARY KEY,
    phone VARCHAR(15) NOT NULL,
    otp VARCHAR(6) NOT NULL,
    purpose VARCHAR(20) DEFAULT 'LOGIN',
    is_used BOOLEAN DEFAULT FALSE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_otps_phone ON otps(phone, is_used, expires_at);

-- ============================================
-- SESSIONS TABLE (JWT token tracking)
-- ============================================
CREATE TABLE sessions (
    id SERIAL PRIMARY KEY,
    user_phone VARCHAR(15) NOT NULL REFERENCES users(phone) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    device_info TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sessions_user ON sessions(user_phone, is_active);

-- ============================================
-- WHATSAPP_MESSAGES TABLE (for logging)
-- ============================================
CREATE TABLE whatsapp_messages (
    id SERIAL PRIMARY KEY,
    message_id VARCHAR(255) UNIQUE,
    from_phone VARCHAR(15) NOT NULL,
    to_phone VARCHAR(15),
    shop_id VARCHAR(50) REFERENCES shops(id) ON DELETE SET NULL,
    direction VARCHAR(10) CHECK (direction IN ('INBOUND', 'OUTBOUND')),
    message_type VARCHAR(20),
    content TEXT,
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_wa_messages_phone ON whatsapp_messages(from_phone, created_at DESC);

-- ============================================
-- FUNCTIONS & TRIGGERS
-- ============================================

-- Update timestamp trigger function
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to all tables with updated_at
CREATE TRIGGER update_shops_timestamp BEFORE UPDATE ON shops FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER update_users_timestamp BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER update_products_timestamp BEFORE UPDATE ON products FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER update_cart_items_timestamp BEFORE UPDATE ON cart_items FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER update_orders_timestamp BEFORE UPDATE ON orders FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER update_payments_timestamp BEFORE UPDATE ON payments FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ============================================
-- SEED DATA (for testing)
-- ============================================

-- Insert sample shop
INSERT INTO shops (id, name, owner_phone, owner_name, address, city, whatsapp_number)
VALUES ('SHOP_001', 'Rama Kirana Store', '9876543210', 'Rama Krishna', 'Dwaraka Nagar, Vizag', 'Vizag', '9876543210');

-- Insert shop owner user
INSERT INTO users (phone, name, role, shop_id)
VALUES ('9876543210', 'Rama Krishna', 'SHOP_OWNER', 'SHOP_001');

-- Insert sample products
INSERT INTO products (id, shop_id, name, price, unit, category, is_available) VALUES
('P_001', 'SHOP_001', 'Rice (1 kg)', 50.00, 'kg', 'Groceries', true),
('P_002', 'SHOP_001', 'Toor Dal (1 kg)', 120.00, 'kg', 'Groceries', true),
('P_003', 'SHOP_001', 'Sugar (1 kg)', 45.00, 'kg', 'Groceries', true),
('P_004', 'SHOP_001', 'Sunflower Oil (1 L)', 140.00, 'litre', 'Groceries', true),
('P_005', 'SHOP_001', 'Wheat Flour (1 kg)', 40.00, 'kg', 'Groceries', true),
('P_006', 'SHOP_001', 'Salt (1 kg)', 20.00, 'kg', 'Groceries', true),
('P_007', 'SHOP_001', 'Tea Powder (250g)', 80.00, 'pack', 'Beverages', true),
('P_008', 'SHOP_001', 'Coffee Powder (200g)', 150.00, 'pack', 'Beverages', true),
('P_009', 'SHOP_001', 'Milk (500ml)', 25.00, 'pack', 'Dairy', true),
('P_010', 'SHOP_001', 'Bread', 35.00, 'pack', 'Bakery', true),
-- Vegetable Products (priced per kg)
('P_V01', 'SHOP_001', 'Tomato', 40.00, 'kg', 'Vegetables', true),
('P_V02', 'SHOP_001', 'Onion', 35.00, 'kg', 'Vegetables', true),
('P_V03', 'SHOP_001', 'Potato', 30.00, 'kg', 'Vegetables', true),
('P_V04', 'SHOP_001', 'Carrot', 50.00, 'kg', 'Vegetables', true),
('P_V05', 'SHOP_001', 'Capsicum', 80.00, 'kg', 'Vegetables', true),
('P_V06', 'SHOP_001', 'Cabbage', 25.00, 'kg', 'Vegetables', true),
('P_V07', 'SHOP_001', 'Cauliflower', 40.00, 'kg', 'Vegetables', true),
('P_V08', 'SHOP_001', 'Beans', 60.00, 'kg', 'Vegetables', true),
('P_V09', 'SHOP_001', 'Brinjal', 35.00, 'kg', 'Vegetables', true),
('P_V10', 'SHOP_001', 'Ladies Finger (Bhindi)', 45.00, 'kg', 'Vegetables', true);

-- Insert sample customer
INSERT INTO users (phone, name, role, default_address)
VALUES ('9988776655', 'Test Customer', 'CUSTOMER', 'MVP Colony, Vizag');

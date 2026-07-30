-- Clean SQL DDL Script for MySQL Workbench (UTF-8 Encoded)
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    phone_number VARCHAR(20),
    role VARCHAR(50) DEFAULT 'customer'
);

CREATE TABLE restaurant_tables (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_number VARCHAR(50) NOT NULL UNIQUE,
    capacity INT DEFAULT 4,
    status VARCHAR(50) DEFAULT 'available',
    zone VARCHAR(10) DEFAULT 'A'
);

CREATE TABLE menu_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    food_name VARCHAR(100) NOT NULL,
    price DOUBLE NOT NULL,
    category VARCHAR(50),
    is_available TINYINT(1) DEFAULT 1
);

CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_code VARCHAR(20) NOT NULL UNIQUE,
    customer_username VARCHAR(255),
    guest_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    table_summary VARCHAR(250) NOT NULL,
    total_amount DOUBLE DEFAULT 0,
    status VARCHAR(50) DEFAULT 'pending',
    payment_method VARCHAR(20),
    paid_at DATETIME,
    booking_date DATE,
    booking_time VARCHAR(20),
    deposit_paid TINYINT(1) DEFAULT 0,
    is_merged INT DEFAULT 0,
    table_type VARCHAR(50),
    CONSTRAINT fk_bookings_users FOREIGN KEY (customer_username) REFERENCES users(username) ON DELETE SET NULL
);

CREATE TABLE booking_order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    food_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DOUBLE NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_items_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_menu FOREIGN KEY (food_id) REFERENCES menu_items(id) ON DELETE CASCADE
);

CREATE TABLE invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    booking_code VARCHAR(20),
    guest_name VARCHAR(100),
    guest_phone VARCHAR(20),
    table_summary VARCHAR(200),
    food_subtotal DOUBLE,
    discount_percent DOUBLE,
    discount_amount DOUBLE,
    deposit_amount DOUBLE,
    total_amount DOUBLE,
    payment_method VARCHAR(20),
    paid_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    note TEXT,
    CONSTRAINT fk_invoices_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);

CREATE TABLE dashboard_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    available_tables INT DEFAULT 14,
    occupied_tables INT DEFAULT 6,
    reserved_tables INT DEFAULT 5,
    cleaning_tables INT DEFAULT 2,
    guests_in INT DEFAULT 28,
    guests_out INT DEFAULT 22,
    revenue VARCHAR(50) DEFAULT '5600000',
    alerts TEXT
);

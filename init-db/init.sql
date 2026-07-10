CREATE DATABASE IF NOT EXISTS order_db;
CREATE DATABASE IF NOT EXISTS inventory_db;
CREATE DATABASE IF NOT EXISTS payment_db;
CREATE DATABASE IF NOT EXISTS notification_db;

USE order_db;
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    amount DECIMAL(38,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL
);

USE inventory_db;
CREATE TABLE IF NOT EXISTS inventory (
    product_id BIGINT PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    available_quantity INT NOT NULL,
    reserved_quantity INT NOT NULL
);

-- Seed initial stock for testing
INSERT INTO inventory (product_id, product_name, available_quantity, reserved_quantity)
VALUES 
(1, 'Gaming Laptop', 100, 0),
(2, 'Wireless Mouse', 500, 0),
(3, 'Mechanical Keyboard', 250, 0)
ON DUPLICATE KEY UPDATE product_name=VALUES(product_name);

USE payment_db;
CREATE TABLE IF NOT EXISTS payments (
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    amount DECIMAL(38,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS failed_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    topic_name VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    error_message TEXT,
    failed_at DATETIME NOT NULL
);

USE notification_db;
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    message VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL
);

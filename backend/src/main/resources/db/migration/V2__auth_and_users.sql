-- Create roles table
CREATE TABLE roles (
    id VARCHAR(50) PRIMARY KEY,
    description VARCHAR(255) NOT NULL
);

-- Seed standard roles with the ROLE_ prefix
INSERT INTO roles (id, description) VALUES
('ROLE_ADMIN', 'System Administrator with full access'),
('ROLE_OPERATIONS', 'Operations manager for inventory and challans'),
('ROLE_ACCOUNTS', 'Financial manager for billing, payments, and GST'),
('ROLE_VIEWER', 'Read-only access across the system');

-- Create users table with case-insensitive unique constraints
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    username VARCHAR(50) COLLATE utf8mb4_unicode_ci NOT NULL UNIQUE,
    email VARCHAR(100) COLLATE utf8mb4_unicode_ci NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

-- Create user_roles join table
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Create user_activity_logs table (No foreign key to avoid transaction deadlocks on references)
CREATE TABLE user_activity_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    username_snapshot VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NULL,
    entity_id VARCHAR(50) NULL,
    description VARCHAR(255) NOT NULL,
    request_method VARCHAR(10) NULL,
    request_path VARCHAR(255) NULL,
    ip_address VARCHAR(45) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create database indexes for optimized queries
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_active ON users(active);
CREATE INDEX idx_activity_logs_user_id ON user_activity_logs(user_id);
CREATE INDEX idx_activity_logs_action ON user_activity_logs(action);
CREATE INDEX idx_activity_logs_created_at ON user_activity_logs(created_at);

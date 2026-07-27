ALTER TABLE site_orders DROP CONSTRAINT ck_site_orders_status;
ALTER TABLE site_orders ADD CONSTRAINT ck_site_orders_status CHECK (status IN ('DRAFT','CONFIRMED','PARTIALLY_FULFILLED','FULFILLED','COMPLETED','CANCELLED'));

CREATE TABLE site_stock_balances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    site_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    pending_quantity DECIMAL(19,4) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_site_stock_balances UNIQUE (site_id, item_id),
    CONSTRAINT fk_site_stock_balances_site FOREIGN KEY (site_id) REFERENCES sites(id),
    CONSTRAINT fk_site_stock_balances_item FOREIGN KEY (item_id) REFERENCES items(id),
    CONSTRAINT ck_site_stock_balances_pending CHECK (pending_quantity >= 0)
);

CREATE TABLE issued_challans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    challan_number VARCHAR(50) NOT NULL,
    site_order_id BIGINT NOT NULL,
    dispatch_date DATE NOT NULL,
    vehicle_number VARCHAR(50) NULL,
    driver_name VARCHAR(100) NULL,
    notes VARCHAR(1000) NULL,
    created_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_issued_challans_number UNIQUE (challan_number),
    CONSTRAINT fk_issued_challans_order FOREIGN KEY (site_order_id) REFERENCES site_orders(id)
);

CREATE TABLE issued_challan_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    issued_challan_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    quantity DECIMAL(19,4) NOT NULL,
    item_code_snapshot VARCHAR(50) NOT NULL,
    item_name_snapshot VARCHAR(255) NOT NULL,
    unit_snapshot VARCHAR(20) NOT NULL,
    CONSTRAINT fk_issued_challan_items_challan FOREIGN KEY (issued_challan_id) REFERENCES issued_challans(id),
    CONSTRAINT fk_issued_challan_items_item FOREIGN KEY (item_id) REFERENCES items(id),
    CONSTRAINT ck_issued_challan_items_qty CHECK (quantity > 0)
);

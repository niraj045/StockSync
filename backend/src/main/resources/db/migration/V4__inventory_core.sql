CREATE TABLE stock_balances (
    item_id BIGINT PRIMARY KEY,
    available_quantity DECIMAL(19,4) NOT NULL DEFAULT 0,
    issued_quantity DECIMAL(19,4) NOT NULL DEFAULT 0,
    hired_quantity DECIMAL(19,4) NOT NULL DEFAULT 0,
    lost_quantity DECIMAL(19,4) NOT NULL DEFAULT 0,
    scrapped_quantity DECIMAL(19,4) NOT NULL DEFAULT 0,
    available_weight DECIMAL(19,4) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_stock_balances_item FOREIGN KEY (item_id) REFERENCES items(id)
);

CREATE TABLE purchases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_number VARCHAR(50) NOT NULL,
    vendor_id BIGINT NOT NULL,
    purchase_date DATE NOT NULL,
    notes VARCHAR(1000) NULL,
    total_value DECIMAL(19,2) NOT NULL DEFAULT 0,
    idempotency_key VARCHAR(100) NOT NULL,
    created_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_purchases_number UNIQUE (purchase_number),
    CONSTRAINT uk_purchases_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_purchases_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id)
);

CREATE TABLE purchase_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    quantity DECIMAL(19,4) NOT NULL,
    unit_rate DECIMAL(19,2) NOT NULL,
    line_value DECIMAL(19,2) NOT NULL,
    CONSTRAINT fk_purchase_items_purchase FOREIGN KEY (purchase_id) REFERENCES purchases(id),
    CONSTRAINT fk_purchase_items_item FOREIGN KEY (item_id) REFERENCES items(id),
    CONSTRAINT ck_purchase_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_purchase_items_rate CHECK (unit_rate >= 0)
);

CREATE TABLE scrap_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scrap_number VARCHAR(50) NOT NULL,
    scrap_date DATE NOT NULL,
    reason VARCHAR(500) NOT NULL,
    notes VARCHAR(1000) NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    created_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_scrap_entries_number UNIQUE (scrap_number),
    CONSTRAINT uk_scrap_entries_idempotency UNIQUE (idempotency_key)
);

CREATE TABLE scrap_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scrap_entry_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    quantity DECIMAL(19,4) NOT NULL,
    CONSTRAINT fk_scrap_items_entry FOREIGN KEY (scrap_entry_id) REFERENCES scrap_entries(id),
    CONSTRAINT fk_scrap_items_item FOREIGN KEY (item_id) REFERENCES items(id),
    CONSTRAINT ck_scrap_items_quantity CHECK (quantity > 0)
);

CREATE TABLE stock_adjustments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    adjustment_number VARCHAR(50) NOT NULL,
    adjustment_date DATE NOT NULL,
    direction VARCHAR(10) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    notes VARCHAR(1000) NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    created_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_stock_adjustments_number UNIQUE (adjustment_number),
    CONSTRAINT uk_stock_adjustments_idempotency UNIQUE (idempotency_key),
    CONSTRAINT ck_stock_adjustments_direction CHECK (direction IN ('IN', 'OUT'))
);

CREATE TABLE stock_adjustment_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    adjustment_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    quantity DECIMAL(19,4) NOT NULL,
    CONSTRAINT fk_adjustment_items_adjustment FOREIGN KEY (adjustment_id) REFERENCES stock_adjustments(id),
    CONSTRAINT fk_adjustment_items_item FOREIGN KEY (item_id) REFERENCES items(id),
    CONSTRAINT ck_adjustment_items_quantity CHECK (quantity > 0)
);

CREATE TABLE stock_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    transaction_date DATE NOT NULL,
    quantity DECIMAL(19,4) NOT NULL,
    weight DECIMAL(19,4) NULL,
    direction VARCHAR(3) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    source_id BIGINT NOT NULL,
    site_id BIGINT NULL,
    party_id BIGINT NULL,
    notes VARCHAR(1000) NULL,
    created_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    reversal_of_transaction_id BIGINT NULL,
    CONSTRAINT fk_stock_transactions_item FOREIGN KEY (item_id) REFERENCES items(id),
    CONSTRAINT fk_stock_transactions_site FOREIGN KEY (site_id) REFERENCES sites(id),
    CONSTRAINT fk_stock_transactions_party FOREIGN KEY (party_id) REFERENCES parties(id),
    CONSTRAINT fk_stock_transactions_reversal FOREIGN KEY (reversal_of_transaction_id) REFERENCES stock_transactions(id),
    CONSTRAINT ck_stock_transactions_quantity CHECK (quantity > 0),
    CONSTRAINT ck_stock_transactions_direction CHECK (direction IN ('IN', 'OUT'))
);

CREATE INDEX idx_stock_transactions_item_date ON stock_transactions(item_id, transaction_date);
CREATE INDEX idx_stock_transactions_type ON stock_transactions(transaction_type);
CREATE INDEX idx_stock_transactions_source ON stock_transactions(source_type, source_id);
CREATE INDEX idx_purchases_date ON purchases(purchase_date);
CREATE INDEX idx_scrap_entries_date ON scrap_entries(scrap_date);
CREATE INDEX idx_stock_adjustments_date ON stock_adjustments(adjustment_date);

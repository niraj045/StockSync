CREATE TABLE stock_import_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_code VARCHAR(80) NOT NULL,
    import_type VARCHAR(40) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    file_checksum VARCHAR(64) NOT NULL,
    source_format VARCHAR(50) NOT NULL,
    party_snapshot_date DATE NOT NULL,
    godown_snapshot_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_source_rows INT NOT NULL DEFAULT 0,
    total_balance_rows INT NOT NULL DEFAULT 0,
    valid_rows INT NOT NULL DEFAULT 0,
    warning_rows INT NOT NULL DEFAULT 0,
    error_rows INT NOT NULL DEFAULT 0,
    expected_party_total DECIMAL(19,4) NOT NULL DEFAULT 0,
    expected_godown_total DECIMAL(19,4) NOT NULL DEFAULT 0,
    imported_at TIMESTAMP(6) NULL,
    imported_by VARCHAR(50) NULL,
    reversed_at TIMESTAMP(6) NULL,
    reversed_by VARCHAR(50) NULL,
    reversal_reason VARCHAR(500) NULL,
    notes VARCHAR(1000) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_stock_import_batches_code UNIQUE (batch_code),
    CONSTRAINT uk_stock_import_batches_checksum UNIQUE (file_checksum),
    CONSTRAINT ck_stock_import_batches_type CHECK (import_type = 'CLIENT_OPENING_STOCK'),
    CONSTRAINT ck_stock_import_batches_source CHECK (source_format = 'STEELFAB_STOCK_SNAPSHOT_V1'),
    CONSTRAINT ck_stock_import_batches_status CHECK (status IN
        ('UPLOADED','PARSED','MAPPING_REQUIRED','VALIDATED','POSTED','PARTIALLY_POSTED','FAILED','REVERSED'))
);

CREATE TABLE stock_import_rows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    source_excel_row INT NOT NULL,
    source_excel_column VARCHAR(5) NOT NULL,
    source_sr_number VARCHAR(30) NULL,
    source_item_name VARCHAR(255) NOT NULL,
    normalized_item_suggestion VARCHAR(255) NULL,
    mapped_item_id BIGINT NULL,
    source_location_name VARCHAR(255) NOT NULL,
    mapped_party_id BIGINT NULL,
    mapped_site_id BIGINT NULL,
    mapped_godown_code VARCHAR(50) NULL,
    location_type VARCHAR(20) NOT NULL,
    quantity DECIMAL(19,4) NOT NULL,
    snapshot_date DATE NOT NULL,
    target_stock_bucket VARCHAR(20) NOT NULL,
    opening_transaction_type VARCHAR(40) NOT NULL,
    validation_status VARCHAR(30) NOT NULL,
    validation_message VARCHAR(1000) NULL,
    duplicate_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    excluded BOOLEAN NOT NULL DEFAULT FALSE,
    exclusion_reason VARCHAR(500) NULL,
    posted_stock_transaction_id BIGINT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_stock_import_rows_batch FOREIGN KEY (batch_id) REFERENCES stock_import_batches(id),
    CONSTRAINT fk_stock_import_rows_item FOREIGN KEY (mapped_item_id) REFERENCES items(id),
    CONSTRAINT fk_stock_import_rows_party FOREIGN KEY (mapped_party_id) REFERENCES parties(id),
    CONSTRAINT fk_stock_import_rows_site FOREIGN KEY (mapped_site_id) REFERENCES sites(id),
    CONSTRAINT fk_stock_import_rows_posted_transaction FOREIGN KEY (posted_stock_transaction_id) REFERENCES stock_transactions(id),
    CONSTRAINT uk_stock_import_rows_cell UNIQUE (batch_id, source_excel_row, source_excel_column),
    CONSTRAINT ck_stock_import_rows_quantity CHECK (quantity > 0),
    CONSTRAINT ck_stock_import_rows_location CHECK (location_type IN ('GODOWN','PARTY_OR_SITE')),
    CONSTRAINT ck_stock_import_rows_bucket CHECK (target_stock_bucket IN ('AVAILABLE','ISSUED')),
    CONSTRAINT ck_stock_import_rows_transaction CHECK (opening_transaction_type IN ('OPENING_GODOWN_BALANCE','OPENING_SITE_BALANCE')),
    CONSTRAINT ck_stock_import_rows_validation CHECK (validation_status IN
        ('READY','MAPPING_REQUIRED','WARNING','ERROR','POSTED','REVERSED'))
);

CREATE TABLE stock_import_location_mappings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    source_excel_column VARCHAR(5) NOT NULL,
    source_location_name VARCHAR(255) NOT NULL,
    mapped_party_id BIGINT NOT NULL,
    mapped_site_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_import_location_batch FOREIGN KEY (batch_id) REFERENCES stock_import_batches(id),
    CONSTRAINT fk_import_location_party FOREIGN KEY (mapped_party_id) REFERENCES parties(id),
    CONSTRAINT fk_import_location_site FOREIGN KEY (mapped_site_id) REFERENCES sites(id),
    CONSTRAINT uk_import_location_column UNIQUE (batch_id, source_excel_column)
);

CREATE TABLE item_aliases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    alias VARCHAR(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    source_system VARCHAR(80) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_item_aliases_item FOREIGN KEY (item_id) REFERENCES items(id),
    CONSTRAINT uk_item_alias_source UNIQUE (alias, source_system)
);

ALTER TABLE stock_transactions
    ADD COLUMN stock_bucket VARCHAR(20) NULL AFTER direction,
    ADD COLUMN import_batch_id BIGINT NULL AFTER source_id,
    ADD COLUMN import_row_id BIGINT NULL AFTER import_batch_id,
    ADD CONSTRAINT fk_stock_transactions_import_batch FOREIGN KEY (import_batch_id) REFERENCES stock_import_batches(id),
    ADD CONSTRAINT fk_stock_transactions_import_row FOREIGN KEY (import_row_id) REFERENCES stock_import_rows(id);

CREATE INDEX idx_stock_import_batches_status ON stock_import_batches(status);
CREATE INDEX idx_stock_import_rows_batch_status ON stock_import_rows(batch_id, validation_status);
CREATE INDEX idx_stock_import_rows_item ON stock_import_rows(mapped_item_id);
CREATE INDEX idx_stock_import_rows_location ON stock_import_rows(batch_id, source_excel_column);
CREATE INDEX idx_item_aliases_item ON item_aliases(item_id);
CREATE INDEX idx_stock_transactions_import ON stock_transactions(import_batch_id, import_row_id);

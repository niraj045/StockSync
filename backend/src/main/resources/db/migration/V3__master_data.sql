CREATE TABLE item_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(50) NOT NULL,
    CONSTRAINT uk_item_categories_name UNIQUE (name)
);

CREATE TABLE items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_code VARCHAR(50) NOT NULL,
    item_name VARCHAR(150) NOT NULL,
    category_id BIGINT NOT NULL,
    size VARCHAR(100) NULL,
    unit VARCHAR(30) NOT NULL,
    weight_per_piece DECIMAL(19,4) NULL,
    purchase_value DECIMAL(19,2) NULL,
    rental_configuration VARCHAR(500) NULL,
    loss_rate DECIMAL(19,2) NULL,
    scrap_value DECIMAL(19,2) NULL,
    minimum_stock DECIMAL(19,4) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(50) NOT NULL,
    CONSTRAINT uk_items_code UNIQUE (item_code),
    CONSTRAINT fk_items_category FOREIGN KEY (category_id) REFERENCES item_categories(id)
);

CREATE TABLE parties (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    legal_name VARCHAR(150) NOT NULL,
    trade_name VARCHAR(150) NULL,
    gstin VARCHAR(15) NULL,
    pan VARCHAR(10) NULL,
    contact_person VARCHAR(100) NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(150) NULL,
    address VARCHAR(500) NULL,
    state VARCHAR(100) NULL,
    notes VARCHAR(1000) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(50) NOT NULL,
    CONSTRAINT uk_parties_gstin UNIQUE (gstin),
    CONSTRAINT uk_parties_pan UNIQUE (pan)
);

CREATE TABLE vendors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    gstin VARCHAR(15) NULL,
    contact_person VARCHAR(100) NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(150) NULL,
    address VARCHAR(500) NULL,
    notes VARCHAR(1000) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(50) NOT NULL,
    CONSTRAINT uk_vendors_name UNIQUE (name),
    CONSTRAINT uk_vendors_gstin UNIQUE (gstin)
);

CREATE TABLE sites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    party_id BIGINT NOT NULL,
    site_name VARCHAR(150) NOT NULL,
    site_code VARCHAR(50) NOT NULL,
    address VARCHAR(500) NULL,
    contact_person VARCHAR(100) NULL,
    start_date DATE NULL,
    expected_end_date DATE NULL,
    status VARCHAR(20) NOT NULL,
    defaulter BOOLEAN NOT NULL DEFAULT FALSE,
    closed_date DATE NULL,
    notes VARCHAR(1000) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(50) NOT NULL,
    CONSTRAINT uk_sites_code UNIQUE (site_code),
    CONSTRAINT fk_sites_party FOREIGN KEY (party_id) REFERENCES parties(id),
    CONSTRAINT ck_sites_status CHECK (status IN ('ACTIVE', 'ON_HOLD', 'DEFAULTER', 'CLOSED'))
);

CREATE TABLE file_attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_type VARCHAR(30) NOT NULL,
    entity_id BIGINT NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    description VARCHAR(500) NULL,
    uploaded_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_file_attachments_stored_name UNIQUE (stored_filename),
    CONSTRAINT ck_file_attachments_entity_type CHECK (entity_type IN ('PARTY', 'SITE', 'VENDOR', 'ITEM'))
);

CREATE INDEX idx_items_category ON items(category_id);
CREATE INDEX idx_items_active ON items(active);
CREATE INDEX idx_items_name ON items(item_name);
CREATE INDEX idx_parties_active ON parties(active);
CREATE INDEX idx_parties_legal_name ON parties(legal_name);
CREATE INDEX idx_vendors_active ON vendors(active);
CREATE INDEX idx_sites_party ON sites(party_id);
CREATE INDEX idx_sites_status ON sites(status);
CREATE INDEX idx_sites_defaulter ON sites(defaulter);
CREATE INDEX idx_file_attachments_entity ON file_attachments(entity_type, entity_id);

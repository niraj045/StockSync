CREATE TABLE agreement_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500) NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(50) NOT NULL,
    CONSTRAINT uk_agreement_templates_name UNIQUE (name),
    CONSTRAINT uk_agreement_templates_stored_filename UNIQUE (stored_filename)
);

CREATE TABLE quotations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quotation_number VARCHAR(50) NOT NULL,
    party_id BIGINT NOT NULL,
    site_id BIGINT NOT NULL,
    quotation_date DATE NOT NULL,
    valid_until DATE NOT NULL,
    rental_type VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    transport_charge DECIMAL(19,2) NOT NULL DEFAULT 0,
    loading_charge DECIMAL(19,2) NOT NULL DEFAULT 0,
    unloading_charge DECIMAL(19,2) NOT NULL DEFAULT 0,
    tax_rate DECIMAL(7,4) NOT NULL DEFAULT 0,
    subtotal DECIMAL(19,2) NOT NULL DEFAULT 0,
    tax_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    grand_total DECIMAL(19,2) NOT NULL DEFAULT 0,
    terms VARCHAR(4000) NULL,
    notes VARCHAR(1000) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(50) NOT NULL,
    CONSTRAINT uk_quotations_number UNIQUE (quotation_number),
    CONSTRAINT fk_quotations_party FOREIGN KEY (party_id) REFERENCES parties(id),
    CONSTRAINT fk_quotations_site FOREIGN KEY (site_id) REFERENCES sites(id),
    CONSTRAINT ck_quotations_status CHECK (status IN ('DRAFT','SENT','APPROVED','REJECTED','EXPIRED','CONVERTED')),
    CONSTRAINT ck_quotations_rental_type CHECK (rental_type IN ('PER_PIECE_PER_DAY','PLATE_AREA_PER_DAY','SCAFFOLD_AREA_PER_DAY','PLOT_AREA_PER_DAY','FIXED_RATE','SLAB_BASED')),
    CONSTRAINT ck_quotations_dates CHECK (valid_until >= quotation_date),
    CONSTRAINT ck_quotations_amounts CHECK (
        transport_charge >= 0 AND loading_charge >= 0 AND unloading_charge >= 0
        AND tax_rate >= 0 AND tax_rate <= 100 AND subtotal >= 0 AND tax_amount >= 0 AND grand_total >= 0
    )
);

CREATE TABLE quotation_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quotation_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    quantity DECIMAL(19,4) NOT NULL,
    unit_rate DECIMAL(19,2) NOT NULL,
    rental_rate DECIMAL(19,4) NOT NULL,
    line_amount DECIMAL(19,2) NOT NULL,
    notes VARCHAR(500) NULL,
    CONSTRAINT fk_quotation_items_quotation FOREIGN KEY (quotation_id) REFERENCES quotations(id),
    CONSTRAINT fk_quotation_items_item FOREIGN KEY (item_id) REFERENCES items(id),
    CONSTRAINT uk_quotation_items_item UNIQUE (quotation_id, item_id),
    CONSTRAINT ck_quotation_items_values CHECK (quantity > 0 AND unit_rate >= 0 AND rental_rate >= 0 AND line_amount >= 0)
);

CREATE TABLE agreements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agreement_number VARCHAR(50) NOT NULL,
    quotation_id BIGINT NULL,
    template_id BIGINT NULL,
    party_id BIGINT NOT NULL,
    site_id BIGINT NOT NULL,
    effective_date DATE NOT NULL,
    expiry_date DATE NULL,
    rental_type VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    security_deposit DECIMAL(19,2) NOT NULL DEFAULT 0,
    transport_charge DECIMAL(19,2) NOT NULL DEFAULT 0,
    loading_charge DECIMAL(19,2) NOT NULL DEFAULT 0,
    unloading_charge DECIMAL(19,2) NOT NULL DEFAULT 0,
    terms VARCHAR(4000) NULL,
    notes VARCHAR(1000) NULL,
    generated_filename VARCHAR(255) NULL,
    generated_storage_path VARCHAR(500) NULL,
    generated_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(50) NOT NULL,
    CONSTRAINT uk_agreements_number UNIQUE (agreement_number),
    CONSTRAINT uk_agreements_quotation UNIQUE (quotation_id),
    CONSTRAINT fk_agreements_quotation FOREIGN KEY (quotation_id) REFERENCES quotations(id),
    CONSTRAINT fk_agreements_template FOREIGN KEY (template_id) REFERENCES agreement_templates(id),
    CONSTRAINT fk_agreements_party FOREIGN KEY (party_id) REFERENCES parties(id),
    CONSTRAINT fk_agreements_site FOREIGN KEY (site_id) REFERENCES sites(id),
    CONSTRAINT ck_agreements_status CHECK (status IN ('DRAFT','GENERATED','ACTIVE','EXPIRED','TERMINATED')),
    CONSTRAINT ck_agreements_rental_type CHECK (rental_type IN ('PER_PIECE_PER_DAY','PLATE_AREA_PER_DAY','SCAFFOLD_AREA_PER_DAY','PLOT_AREA_PER_DAY','FIXED_RATE','SLAB_BASED')),
    CONSTRAINT ck_agreements_dates CHECK (expiry_date IS NULL OR expiry_date >= effective_date),
    CONSTRAINT ck_agreements_amounts CHECK (security_deposit >= 0 AND transport_charge >= 0 AND loading_charge >= 0 AND unloading_charge >= 0)
);

CREATE TABLE agreement_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agreement_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    agreed_quantity DECIMAL(19,4) NOT NULL,
    unit_rate DECIMAL(19,2) NOT NULL,
    rental_rate DECIMAL(19,4) NOT NULL,
    notes VARCHAR(500) NULL,
    CONSTRAINT fk_agreement_items_agreement FOREIGN KEY (agreement_id) REFERENCES agreements(id),
    CONSTRAINT fk_agreement_items_item FOREIGN KEY (item_id) REFERENCES items(id),
    CONSTRAINT uk_agreement_items_item UNIQUE (agreement_id, item_id),
    CONSTRAINT ck_agreement_items_values CHECK (agreed_quantity > 0 AND unit_rate >= 0 AND rental_rate >= 0)
);

CREATE TABLE site_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL,
    agreement_id BIGINT NOT NULL,
    party_id BIGINT NOT NULL,
    site_id BIGINT NOT NULL,
    order_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    notes VARCHAR(1000) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(50) NOT NULL,
    CONSTRAINT uk_site_orders_number UNIQUE (order_number),
    CONSTRAINT fk_site_orders_agreement FOREIGN KEY (agreement_id) REFERENCES agreements(id),
    CONSTRAINT fk_site_orders_party FOREIGN KEY (party_id) REFERENCES parties(id),
    CONSTRAINT fk_site_orders_site FOREIGN KEY (site_id) REFERENCES sites(id),
    CONSTRAINT ck_site_orders_status CHECK (status IN ('DRAFT','CONFIRMED','COMPLETED','CANCELLED'))
);

CREATE TABLE site_order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    ordered_quantity DECIMAL(19,4) NOT NULL,
    issued_quantity DECIMAL(19,4) NOT NULL DEFAULT 0,
    remaining_quantity DECIMAL(19,4) GENERATED ALWAYS AS (ordered_quantity - issued_quantity) STORED,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_site_order_items_order FOREIGN KEY (order_id) REFERENCES site_orders(id),
    CONSTRAINT fk_site_order_items_item FOREIGN KEY (item_id) REFERENCES items(id),
    CONSTRAINT uk_site_order_items_item UNIQUE (order_id, item_id),
    CONSTRAINT ck_site_order_items_values CHECK (ordered_quantity > 0 AND issued_quantity >= 0 AND issued_quantity <= ordered_quantity)
);

CREATE INDEX idx_quotations_party_site ON quotations(party_id, site_id);
CREATE INDEX idx_quotations_status_date ON quotations(status, quotation_date);
CREATE INDEX idx_agreements_party_site ON agreements(party_id, site_id);
CREATE INDEX idx_agreements_status_date ON agreements(status, effective_date);
CREATE INDEX idx_site_orders_agreement ON site_orders(agreement_id);
CREATE INDEX idx_site_orders_site_status ON site_orders(site_id, status);

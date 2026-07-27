-- Alter stock_balances to include damaged_quantity
ALTER TABLE stock_balances ADD COLUMN damaged_quantity DECIMAL(19,4) NOT NULL DEFAULT 0.0000;

-- Loss Records
CREATE TABLE loss_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loss_number VARCHAR(50) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    source_receiving_challan_id BIGINT NULL,
    source_receiving_challan_item_id BIGINT NULL,
    agreement_id BIGINT NOT NULL,
    party_id BIGINT NOT NULL,
    site_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    loss_date DATE NOT NULL,
    quantity DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    weight DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    charge_method VARCHAR(30) NOT NULL,
    recovery_rate DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    calculated_recovery_amount DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    reason VARCHAR(500) NULL,
    attachment_id BIGINT NULL,
    status VARCHAR(30) NOT NULL,
    approved_at TIMESTAMP(6) NULL,
    approved_by VARCHAR(50) NULL,
    reversed_at TIMESTAMP(6) NULL,
    reversed_by VARCHAR(50) NULL,
    reversal_reason VARCHAR(255) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(50) NOT NULL,
    CONSTRAINT uk_loss_records_number UNIQUE (loss_number),
    CONSTRAINT fk_loss_records_receiving FOREIGN KEY (source_receiving_challan_id) REFERENCES receiving_challans(id),
    CONSTRAINT fk_loss_records_receiving_item FOREIGN KEY (source_receiving_challan_item_id) REFERENCES receiving_challan_items(id),
    CONSTRAINT fk_loss_records_agreement FOREIGN KEY (agreement_id) REFERENCES agreements(id),
    CONSTRAINT fk_loss_records_party FOREIGN KEY (party_id) REFERENCES parties(id),
    CONSTRAINT fk_loss_records_site FOREIGN KEY (site_id) REFERENCES sites(id),
    CONSTRAINT fk_loss_records_item FOREIGN KEY (item_id) REFERENCES items(id),
    CONSTRAINT fk_loss_records_attachment FOREIGN KEY (attachment_id) REFERENCES file_attachments(id)
);

CREATE INDEX idx_loss_records_status ON loss_records(status);
CREATE INDEX idx_loss_records_date ON loss_records(loss_date);
CREATE INDEX idx_loss_records_site_item ON loss_records(site_id, item_id);

-- Damage Records
CREATE TABLE damage_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    damage_number VARCHAR(50) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    source_receiving_challan_id BIGINT NULL,
    source_receiving_challan_item_id BIGINT NULL,
    agreement_id BIGINT NOT NULL,
    party_id BIGINT NOT NULL,
    site_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    damage_date DATE NOT NULL,
    quantity DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    weight DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    repairable BOOLEAN NOT NULL DEFAULT TRUE,
    damage_type VARCHAR(30) NOT NULL,
    condition_notes VARCHAR(500) NULL,
    charge_method VARCHAR(30) NOT NULL,
    damage_rate DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    calculated_damage_amount DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    estimated_repair_cost DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    actual_repair_cost DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    attachment_id BIGINT NULL,
    status VARCHAR(30) NOT NULL,
    recorded_at TIMESTAMP(6) NULL,
    recorded_by VARCHAR(50) NULL,
    repair_started_at TIMESTAMP(6) NULL,
    repair_started_by VARCHAR(50) NULL,
    repaired_at TIMESTAMP(6) NULL,
    repaired_by VARCHAR(50) NULL,
    scrapped_at TIMESTAMP(6) NULL,
    scrapped_by VARCHAR(50) NULL,
    reversed_at TIMESTAMP(6) NULL,
    reversed_by VARCHAR(50) NULL,
    reversal_reason VARCHAR(255) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(50) NOT NULL,
    CONSTRAINT uk_damage_records_number UNIQUE (damage_number),
    CONSTRAINT fk_damage_records_receiving FOREIGN KEY (source_receiving_challan_id) REFERENCES receiving_challans(id),
    CONSTRAINT fk_damage_records_receiving_item FOREIGN KEY (source_receiving_challan_item_id) REFERENCES receiving_challan_items(id),
    CONSTRAINT fk_damage_records_agreement FOREIGN KEY (agreement_id) REFERENCES agreements(id),
    CONSTRAINT fk_damage_records_party FOREIGN KEY (party_id) REFERENCES parties(id),
    CONSTRAINT fk_damage_records_site FOREIGN KEY (site_id) REFERENCES sites(id),
    CONSTRAINT fk_damage_records_item FOREIGN KEY (item_id) REFERENCES items(id),
    CONSTRAINT fk_damage_records_attachment FOREIGN KEY (attachment_id) REFERENCES file_attachments(id)
);

CREATE INDEX idx_damage_records_status ON damage_records(status);
CREATE INDEX idx_damage_records_date ON damage_records(damage_date);
CREATE INDEX idx_damage_records_site_item ON damage_records(site_id, item_id);

-- Item Exchange Records
CREATE TABLE item_exchange_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exchange_number VARCHAR(50) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    source_receiving_challan_id BIGINT NULL,
    source_receiving_challan_item_id BIGINT NULL,
    agreement_id BIGINT NOT NULL,
    party_id BIGINT NOT NULL,
    site_id BIGINT NOT NULL,
    expected_item_id BIGINT NOT NULL,
    actual_item_id BIGINT NOT NULL,
    exchange_date DATE NOT NULL,
    expected_quantity DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    actual_quantity DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    expected_weight DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    actual_weight DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    destination_stock_status VARCHAR(30) NOT NULL,
    reason VARCHAR(500) NULL,
    status VARCHAR(30) NOT NULL,
    posted_at TIMESTAMP(6) NULL,
    posted_by VARCHAR(50) NULL,
    cancelled_at TIMESTAMP(6) NULL,
    cancelled_by VARCHAR(50) NULL,
    cancellation_reason VARCHAR(255) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(50) NOT NULL,
    CONSTRAINT uk_exchange_records_number UNIQUE (exchange_number),
    CONSTRAINT fk_exchange_records_receiving FOREIGN KEY (source_receiving_challan_id) REFERENCES receiving_challans(id),
    CONSTRAINT fk_exchange_records_receiving_item FOREIGN KEY (source_receiving_challan_item_id) REFERENCES receiving_challan_items(id),
    CONSTRAINT fk_exchange_records_agreement FOREIGN KEY (agreement_id) REFERENCES agreements(id),
    CONSTRAINT fk_exchange_records_party FOREIGN KEY (party_id) REFERENCES parties(id),
    CONSTRAINT fk_exchange_records_site FOREIGN KEY (site_id) REFERENCES sites(id),
    CONSTRAINT fk_exchange_records_expected FOREIGN KEY (expected_item_id) REFERENCES items(id),
    CONSTRAINT fk_exchange_records_actual FOREIGN KEY (actual_item_id) REFERENCES items(id)
);

CREATE INDEX idx_exchange_records_status ON item_exchange_records(status);
CREATE INDEX idx_exchange_records_site ON item_exchange_records(site_id);

-- Site Transfers
CREATE TABLE site_transfers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transfer_number VARCHAR(50) NOT NULL,
    source_agreement_id BIGINT NOT NULL,
    destination_agreement_id BIGINT NOT NULL,
    source_party_id BIGINT NOT NULL,
    source_site_id BIGINT NOT NULL,
    destination_party_id BIGINT NOT NULL,
    destination_site_id BIGINT NOT NULL,
    transfer_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    vehicle_number VARCHAR(50) NULL,
    driver_name VARCHAR(100) NULL,
    driver_phone VARCHAR(20) NULL,
    transporter_id BIGINT NULL,
    notes VARCHAR(1000) NULL,
    posted_at TIMESTAMP(6) NULL,
    posted_by VARCHAR(50) NULL,
    cancelled_at TIMESTAMP(6) NULL,
    cancelled_by VARCHAR(50) NULL,
    cancellation_reason VARCHAR(255) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(50) NOT NULL,
    CONSTRAINT uk_site_transfers_number UNIQUE (transfer_number),
    CONSTRAINT fk_site_transfers_source_ag FOREIGN KEY (source_agreement_id) REFERENCES agreements(id),
    CONSTRAINT fk_site_transfers_dest_ag FOREIGN KEY (destination_agreement_id) REFERENCES agreements(id),
    CONSTRAINT fk_site_transfers_source_pt FOREIGN KEY (source_party_id) REFERENCES parties(id),
    CONSTRAINT fk_site_transfers_source_st FOREIGN KEY (source_site_id) REFERENCES sites(id),
    CONSTRAINT fk_site_transfers_dest_pt FOREIGN KEY (destination_party_id) REFERENCES parties(id),
    CONSTRAINT fk_site_transfers_dest_st FOREIGN KEY (destination_site_id) REFERENCES sites(id)
);

-- Site Transfer Items
CREATE TABLE site_transfer_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transfer_id BIGINT NOT NULL,
    source_agreement_item_id BIGINT NOT NULL,
    destination_agreement_item_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    item_code_snapshot VARCHAR(50) NOT NULL,
    item_name_snapshot VARCHAR(255) NOT NULL,
    description_snapshot VARCHAR(500) NULL,
    size_snapshot VARCHAR(50) NULL,
    unit_snapshot VARCHAR(20) NOT NULL,
    weight_per_piece_snapshot DECIMAL(19,4) NOT NULL,
    quantity DECIMAL(19,4) NOT NULL,
    total_weight DECIMAL(19,4) NOT NULL,
    source_pending_before DECIMAL(19,4) NOT NULL,
    source_pending_after DECIMAL(19,4) NOT NULL,
    destination_pending_before DECIMAL(19,4) NOT NULL,
    destination_pending_after DECIMAL(19,4) NOT NULL,
    sequence INT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_transfer_items_transfer FOREIGN KEY (transfer_id) REFERENCES site_transfers(id),
    CONSTRAINT fk_transfer_items_source_ag_item FOREIGN KEY (source_agreement_item_id) REFERENCES agreement_items(id),
    CONSTRAINT fk_transfer_items_dest_ag_item FOREIGN KEY (destination_agreement_item_id) REFERENCES agreement_items(id),
    CONSTRAINT fk_transfer_items_item FOREIGN KEY (item_id) REFERENCES items(id)
);

CREATE INDEX idx_site_transfers_status ON site_transfers(status);
CREATE INDEX idx_site_transfers_date ON site_transfers(transfer_date);
CREATE INDEX idx_site_transfers_sites ON site_transfers(source_site_id, destination_site_id);

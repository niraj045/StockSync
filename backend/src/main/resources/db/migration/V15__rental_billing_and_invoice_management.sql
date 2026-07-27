-- Extension of agreements and agreement_items
ALTER TABLE loss_records MODIFY agreement_id BIGINT NULL;
ALTER TABLE damage_records MODIFY agreement_id BIGINT NULL;
ALTER TABLE item_exchange_records MODIFY agreement_id BIGINT NULL;
ALTER TABLE file_attachments DROP CHECK ck_file_attachments_entity_type;
ALTER TABLE file_attachments ADD CONSTRAINT ck_file_attachments_entity_type CHECK (entity_type IN ('PARTY', 'SITE', 'VENDOR', 'ITEM', 'AGREEMENT', 'INVOICE'));

ALTER TABLE agreements ADD COLUMN billing_start_rule VARCHAR(30) NOT NULL DEFAULT 'ISSUE_DATE_INCLUDED';
ALTER TABLE agreements ADD COLUMN billing_end_rule VARCHAR(30) NOT NULL DEFAULT 'RETURN_DATE_EXCLUDED';
ALTER TABLE agreement_items ADD COLUMN area DECIMAL(19,4) NULL;

-- Slabs for Agreement Items
CREATE TABLE agreement_item_slabs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agreement_item_id BIGINT NOT NULL,
    start_day INT NOT NULL,
    end_day INT NULL,
    rate DECIMAL(19,4) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_agreement_item_slabs_item FOREIGN KEY (agreement_item_id) REFERENCES agreement_items(id) ON DELETE CASCADE
);

-- Operational charge columns in dispatches, returns, and transfers
ALTER TABLE issued_challans ADD COLUMN transport_charge DECIMAL(19,2) NOT NULL DEFAULT 0;
ALTER TABLE issued_challans ADD COLUMN loading_charge DECIMAL(19,2) NOT NULL DEFAULT 0;
ALTER TABLE issued_challans ADD COLUMN unloading_charge DECIMAL(19,2) NOT NULL DEFAULT 0;

ALTER TABLE receiving_challans ADD COLUMN transport_charge DECIMAL(19,2) NOT NULL DEFAULT 0;
ALTER TABLE receiving_challans ADD COLUMN handling_charge DECIMAL(19,2) NOT NULL DEFAULT 0;

ALTER TABLE site_transfers ADD COLUMN transport_charge DECIMAL(19,2) NOT NULL DEFAULT 0;

-- Billing Runs table
CREATE TABLE billing_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    billing_run_number VARCHAR(50) NOT NULL,
    agreement_id BIGINT NOT NULL,
    party_id BIGINT NOT NULL,
    site_id BIGINT NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    rental_subtotal DECIMAL(19,2) NOT NULL DEFAULT 0,
    loss_charge_total DECIMAL(19,2) NOT NULL DEFAULT 0,
    damage_charge_total DECIMAL(19,2) NOT NULL DEFAULT 0,
    operational_charge_total DECIMAL(19,2) NOT NULL DEFAULT 0,
    manual_adjustment_total DECIMAL(19,2) NOT NULL DEFAULT 0,
    discount_type VARCHAR(20) NOT NULL DEFAULT 'NONE',
    discount_value DECIMAL(19,4) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    taxable_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    cgst_rate DECIMAL(7,4) NOT NULL DEFAULT 0,
    cgst_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    sgst_rate DECIMAL(7,4) NOT NULL DEFAULT 0,
    sgst_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    igst_rate DECIMAL(7,4) NOT NULL DEFAULT 0,
    igst_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    total_tax DECIMAL(19,2) NOT NULL DEFAULT 0,
    round_off DECIMAL(19,2) NOT NULL DEFAULT 0,
    grand_total DECIMAL(19,2) NOT NULL DEFAULT 0,
    calculated_at TIMESTAMP(6) NULL,
    calculated_by VARCHAR(50) NULL,
    finalized_at TIMESTAMP(6) NULL,
    finalized_by VARCHAR(50) NULL,
    cancelled_at TIMESTAMP(6) NULL,
    cancelled_by VARCHAR(50) NULL,
    cancellation_reason VARCHAR(500) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_billing_runs_number UNIQUE (billing_run_number),
    CONSTRAINT fk_billing_runs_agreement FOREIGN KEY (agreement_id) REFERENCES agreements(id),
    CONSTRAINT fk_billing_runs_party FOREIGN KEY (party_id) REFERENCES parties(id),
    CONSTRAINT fk_billing_runs_site FOREIGN KEY (site_id) REFERENCES sites(id)
);

-- Billing Run Segments table
CREATE TABLE billing_run_segments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    billing_run_id BIGINT NOT NULL,
    agreement_item_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    item_code_snapshot VARCHAR(50) NOT NULL,
    item_name_snapshot VARCHAR(255) NOT NULL,
    size_snapshot VARCHAR(50) NULL,
    unit_snapshot VARCHAR(20) NOT NULL,
    weight_snapshot DECIMAL(19,4) NULL,
    source_issue_reference VARCHAR(100) NOT NULL,
    source_end_reference VARCHAR(100) NULL,
    rental_type VARCHAR(40) NOT NULL,
    quantity DECIMAL(19,4) NOT NULL,
    area DECIMAL(19,4) NULL,
    weight DECIMAL(19,4) NULL,
    segment_start DATE NOT NULL,
    segment_end DATE NOT NULL,
    billable_days INT NOT NULL,
    base_rate DECIMAL(19,4) NOT NULL,
    applied_slab_snapshot VARCHAR(500) NULL,
    amount DECIMAL(19,2) NOT NULL,
    calculation_explanation VARCHAR(1000) NOT NULL,
    sequence_number INT NOT NULL,
    CONSTRAINT fk_billing_run_segments_run FOREIGN KEY (billing_run_id) REFERENCES billing_runs(id) ON DELETE CASCADE,
    CONSTRAINT fk_billing_run_segments_agreement_item FOREIGN KEY (agreement_item_id) REFERENCES agreement_items(id),
    CONSTRAINT fk_billing_run_segments_item FOREIGN KEY (item_id) REFERENCES items(id)
);

-- Billing Run Charges table
CREATE TABLE billing_run_charges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    billing_run_id BIGINT NOT NULL,
    source_type VARCHAR(40) NOT NULL,
    source_id BIGINT NOT NULL,
    source_document_number VARCHAR(50) NOT NULL,
    charge_type VARCHAR(40) NOT NULL,
    description VARCHAR(500) NOT NULL,
    quantity DECIMAL(19,4) NOT NULL DEFAULT 1,
    rate DECIMAL(19,4) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    taxable BOOLEAN NOT NULL DEFAULT TRUE,
    selected BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_billing_run_charges_run FOREIGN KEY (billing_run_id) REFERENCES billing_runs(id) ON DELETE CASCADE
);

-- Source Allocations (to prevent double billing of dispatches/returns/losses/damages/transfers)
CREATE TABLE billing_source_allocations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agreement_id BIGINT NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_id BIGINT NOT NULL,
    billing_run_id BIGINT NOT NULL,
    CONSTRAINT uk_source_allocation UNIQUE (source_type, source_id),
    CONSTRAINT fk_billing_allocations_agreement FOREIGN KEY (agreement_id) REFERENCES agreements(id),
    CONSTRAINT fk_billing_allocations_run FOREIGN KEY (billing_run_id) REFERENCES billing_runs(id) ON DELETE CASCADE
);

-- Invoices table
CREATE TABLE invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_number VARCHAR(50) NOT NULL,
    billing_run_id BIGINT NOT NULL,
    agreement_id BIGINT NOT NULL,
    party_id BIGINT NOT NULL,
    site_id BIGINT NOT NULL,
    invoice_date DATE NOT NULL,
    due_date DATE NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    company_name_snapshot VARCHAR(150) NOT NULL,
    company_address_snapshot VARCHAR(500) NOT NULL,
    company_gstin_snapshot VARCHAR(15) NOT NULL,
    party_legal_name_snapshot VARCHAR(255) NOT NULL,
    party_gstin_snapshot VARCHAR(15) NULL,
    party_pan_snapshot VARCHAR(10) NULL,
    party_address_snapshot VARCHAR(500) NOT NULL,
    party_state_snapshot VARCHAR(100) NOT NULL,
    site_name_snapshot VARCHAR(255) NOT NULL,
    site_code_snapshot VARCHAR(50) NOT NULL,
    site_address_snapshot VARCHAR(500) NOT NULL,
    site_contact_snapshot VARCHAR(255) NULL,
    agreement_number_snapshot VARCHAR(50) NOT NULL,
    subtotal DECIMAL(19,2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    taxable_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    cgst_rate DECIMAL(7,4) NOT NULL DEFAULT 0,
    cgst_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    sgst_rate DECIMAL(7,4) NOT NULL DEFAULT 0,
    sgst_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    igst_rate DECIMAL(7,4) NOT NULL DEFAULT 0,
    igst_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    total_tax DECIMAL(19,2) NOT NULL DEFAULT 0,
    round_off DECIMAL(19,2) NOT NULL DEFAULT 0,
    grand_total DECIMAL(19,2) NOT NULL DEFAULT 0,
    terms VARCHAR(4000) NULL,
    notes VARCHAR(1000) NULL,
    generated_pdf_attachment_id BIGINT NULL,
    issued_at TIMESTAMP(6) NULL,
    issued_by VARCHAR(50) NULL,
    cancelled_at TIMESTAMP(6) NULL,
    cancelled_by VARCHAR(50) NULL,
    cancellation_reason VARCHAR(500) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_invoices_number UNIQUE (invoice_number),
    CONSTRAINT uk_invoices_run UNIQUE (billing_run_id),
    CONSTRAINT fk_invoices_run FOREIGN KEY (billing_run_id) REFERENCES billing_runs(id),
    CONSTRAINT fk_invoices_agreement FOREIGN KEY (agreement_id) REFERENCES agreements(id),
    CONSTRAINT fk_invoices_party FOREIGN KEY (party_id) REFERENCES parties(id),
    CONSTRAINT fk_invoices_site FOREIGN KEY (site_id) REFERENCES sites(id),
    CONSTRAINT fk_invoices_pdf FOREIGN KEY (generated_pdf_attachment_id) REFERENCES file_attachments(id)
);

-- Invoice Line Items table
CREATE TABLE invoice_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    line_type VARCHAR(30) NOT NULL,
    agreement_item_id BIGINT NULL,
    source_type VARCHAR(50) NULL,
    source_id BIGINT NULL,
    source_document_number VARCHAR(50) NULL,
    item_id BIGINT NULL,
    item_code_snapshot VARCHAR(50) NULL,
    item_name_snapshot VARCHAR(255) NULL,
    size_snapshot VARCHAR(50) NULL,
    unit_snapshot VARCHAR(20) NULL,
    description VARCHAR(500) NOT NULL,
    quantity DECIMAL(19,4) NOT NULL,
    area DECIMAL(19,4) NULL,
    weight DECIMAL(19,4) NULL,
    billable_days INT NULL,
    rate DECIMAL(19,4) NOT NULL,
    taxable BOOLEAN NOT NULL DEFAULT TRUE,
    amount DECIMAL(19,2) NOT NULL,
    sequence_number INT NOT NULL,
    CONSTRAINT fk_invoice_items_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);

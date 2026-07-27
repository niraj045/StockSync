ALTER TABLE file_attachments DROP CHECK ck_file_attachments_entity_type;
ALTER TABLE file_attachments ADD CONSTRAINT ck_file_attachments_entity_type CHECK (entity_type IN ('PARTY', 'SITE', 'VENDOR', 'ITEM', 'AGREEMENT', 'INVOICE', 'PAYMENT_RECEIPT', 'SECURITY_DEPOSIT'));

ALTER TABLE invoices
    ADD COLUMN cash_allocated_total DECIMAL(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN tds_allocated_total DECIMAL(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN deposit_adjusted_total DECIMAL(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN outstanding_amount DECIMAL(19,2) NOT NULL DEFAULT 0;

UPDATE invoices SET outstanding_amount = grand_total;

CREATE TABLE payment_receipts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    receipt_number VARCHAR(50) NOT NULL,
    party_id BIGINT NOT NULL,
    site_id BIGINT NULL,
    party_name_snapshot VARCHAR(255) NOT NULL,
    site_name_snapshot VARCHAR(255) NULL,
    payment_date DATE NOT NULL,
    payment_mode VARCHAR(30) NOT NULL,
    reference_number VARCHAR(100) NULL,
    bank_name VARCHAR(150) NULL,
    cheque_number VARCHAR(100) NULL,
    cheque_date DATE NULL,
    cash_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    tds_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    total_settlement_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    unallocated_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    notes VARCHAR(1000) NULL,
    attachment_id BIGINT NULL,
    posted_at TIMESTAMP(6) NULL,
    posted_by VARCHAR(50) NULL,
    reversed_at TIMESTAMP(6) NULL,
    reversed_by VARCHAR(50) NULL,
    reversal_reason VARCHAR(500) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_payment_receipts_number UNIQUE (receipt_number),
    CONSTRAINT fk_payment_receipts_party FOREIGN KEY (party_id) REFERENCES parties(id),
    CONSTRAINT fk_payment_receipts_site FOREIGN KEY (site_id) REFERENCES sites(id),
    CONSTRAINT fk_payment_receipts_attachment FOREIGN KEY (attachment_id) REFERENCES file_attachments(id),
    CONSTRAINT ck_payment_receipts_amounts CHECK (cash_amount >= 0 AND tds_amount >= 0 AND total_settlement_amount = cash_amount + tds_amount AND unallocated_amount >= 0)
);

CREATE TABLE payment_allocations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_receipt_id BIGINT NOT NULL,
    invoice_id BIGINT NOT NULL,
    cash_allocated DECIMAL(19,2) NOT NULL DEFAULT 0,
    tds_allocated DECIMAL(19,2) NOT NULL DEFAULT 0,
    total_allocated DECIMAL(19,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_payment_allocation_invoice UNIQUE (payment_receipt_id, invoice_id),
    CONSTRAINT fk_payment_allocations_receipt FOREIGN KEY (payment_receipt_id) REFERENCES payment_receipts(id),
    CONSTRAINT fk_payment_allocations_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id),
    CONSTRAINT ck_payment_allocations_amounts CHECK (cash_allocated >= 0 AND tds_allocated >= 0 AND total_allocated = cash_allocated + tds_allocated)
);

CREATE TABLE tds_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_receipt_id BIGINT NOT NULL,
    tds_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    deduction_date DATE NULL,
    section VARCHAR(50) NULL,
    certificate_number VARCHAR(100) NULL,
    certificate_date DATE NULL,
    certificate_attachment_id BIGINT NULL,
    verification_status VARCHAR(20) NOT NULL,
    rejection_reason VARCHAR(500) NULL,
    verified_at TIMESTAMP(6) NULL,
    verified_by VARCHAR(50) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_tds_details_payment UNIQUE (payment_receipt_id),
    CONSTRAINT fk_tds_details_payment FOREIGN KEY (payment_receipt_id) REFERENCES payment_receipts(id),
    CONSTRAINT fk_tds_details_attachment FOREIGN KEY (certificate_attachment_id) REFERENCES file_attachments(id),
    CONSTRAINT ck_tds_details_amount CHECK (tds_amount >= 0)
);

CREATE TABLE security_deposit_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    deposit_number VARCHAR(50) NOT NULL,
    agreement_id BIGINT NOT NULL,
    party_id BIGINT NOT NULL,
    site_id BIGINT NOT NULL,
    agreement_number_snapshot VARCHAR(50) NOT NULL,
    party_name_snapshot VARCHAR(255) NOT NULL,
    site_name_snapshot VARCHAR(255) NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    transaction_date DATE NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    payment_mode VARCHAR(30) NULL,
    reference_number VARCHAR(100) NULL,
    related_invoice_id BIGINT NULL,
    source_deposit_transaction_id BIGINT NULL,
    status VARCHAR(20) NOT NULL,
    notes VARCHAR(1000) NULL,
    attachment_id BIGINT NULL,
    posted_at TIMESTAMP(6) NULL,
    posted_by VARCHAR(50) NULL,
    reversed_at TIMESTAMP(6) NULL,
    reversed_by VARCHAR(50) NULL,
    reversal_reason VARCHAR(500) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_security_deposit_number UNIQUE (deposit_number),
    CONSTRAINT fk_security_deposits_agreement FOREIGN KEY (agreement_id) REFERENCES agreements(id),
    CONSTRAINT fk_security_deposits_party FOREIGN KEY (party_id) REFERENCES parties(id),
    CONSTRAINT fk_security_deposits_site FOREIGN KEY (site_id) REFERENCES sites(id),
    CONSTRAINT fk_security_deposits_invoice FOREIGN KEY (related_invoice_id) REFERENCES invoices(id),
    CONSTRAINT fk_security_deposits_source FOREIGN KEY (source_deposit_transaction_id) REFERENCES security_deposit_transactions(id),
    CONSTRAINT fk_security_deposits_attachment FOREIGN KEY (attachment_id) REFERENCES file_attachments(id),
    CONSTRAINT ck_security_deposits_amount CHECK (amount > 0)
);

CREATE TABLE deposit_invoice_allocations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    deposit_transaction_id BIGINT NOT NULL,
    invoice_id BIGINT NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_deposit_invoice_allocation UNIQUE (deposit_transaction_id, invoice_id),
    CONSTRAINT fk_deposit_invoice_allocations_transaction FOREIGN KEY (deposit_transaction_id) REFERENCES security_deposit_transactions(id),
    CONSTRAINT fk_deposit_invoice_allocations_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id),
    CONSTRAINT ck_deposit_invoice_allocations_amount CHECK (amount > 0)
);

CREATE INDEX idx_payment_receipts_party_date ON payment_receipts(party_id, payment_date);
CREATE INDEX idx_payment_receipts_site_status ON payment_receipts(site_id, status);
CREATE INDEX idx_payment_receipts_mode ON payment_receipts(payment_mode);
CREATE INDEX idx_payment_allocations_invoice ON payment_allocations(invoice_id);
CREATE INDEX idx_security_deposits_agreement ON security_deposit_transactions(agreement_id, transaction_date);
CREATE INDEX idx_security_deposits_party_site ON security_deposit_transactions(party_id, site_id);
CREATE INDEX idx_security_deposits_status_type ON security_deposit_transactions(status, transaction_type);
CREATE INDEX idx_deposit_invoice_allocations_invoice ON deposit_invoice_allocations(invoice_id);
CREATE INDEX idx_invoices_outstanding_party ON invoices(party_id, status, outstanding_amount);
CREATE INDEX idx_invoices_outstanding_site ON invoices(site_id, status, outstanding_amount);

CREATE TABLE saved_report_filters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    report_type VARCHAR(80) NOT NULL,
    filter_json JSON NOT NULL,
    owner_user_id BIGINT NULL,
    owner_username VARCHAR(50) NOT NULL,
    shared_flag BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_saved_report_filters_owner FOREIGN KEY (owner_user_id) REFERENCES users(id)
);

CREATE TABLE report_export_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_type VARCHAR(80) NOT NULL,
    export_format VARCHAR(20) NOT NULL,
    filter_json JSON NOT NULL,
    generated_by_user_id BIGINT NULL,
    generated_by_username VARCHAR(50) NOT NULL,
    generated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    storage_path VARCHAR(500) NULL,
    original_filename VARCHAR(255) NULL,
    content_type VARCHAR(120) NULL,
    file_size BIGINT NULL,
    status VARCHAR(20) NOT NULL,
    error_message VARCHAR(1000) NULL,
    CONSTRAINT fk_report_exports_user FOREIGN KEY (generated_by_user_id) REFERENCES users(id),
    CONSTRAINT ck_report_exports_format CHECK (export_format IN ('PDF','EXCEL','CSV')),
    CONSTRAINT ck_report_exports_status CHECK (status IN ('SUCCESS','FAILED'))
);

CREATE TABLE gst_export_config_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    format_code VARCHAR(50) NOT NULL,
    version_label VARCHAR(50) NOT NULL,
    mapping_json JSON NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(50) NOT NULL,
    CONSTRAINT uk_gst_export_config_version UNIQUE (format_code, version_label)
);

INSERT INTO gst_export_config_versions (format_code, version_label, mapping_json, active, created_by)
VALUES
('GSTR1_PREPARATION', 'v1', JSON_OBJECT(
    'supplierGstin', 'company_gstin_snapshot',
    'invoiceNumber', 'invoice_number',
    'invoiceDate', 'invoice_date',
    'customerGstin', 'party_gstin_snapshot',
    'customerName', 'party_legal_name_snapshot',
    'placeOfSupply', 'party_state_snapshot',
    'invoiceValue', 'grand_total',
    'taxableValue', 'taxable_amount',
    'cgst', 'cgst_amount',
    'sgst', 'sgst_amount',
    'igst', 'igst_amount',
    'invoiceType', 'derived_b2b_b2c'
), TRUE, 'migration');

CREATE INDEX idx_saved_report_filters_owner_type ON saved_report_filters(owner_user_id, report_type);
CREATE INDEX idx_saved_report_filters_shared_type ON saved_report_filters(shared_flag, report_type);
CREATE INDEX idx_report_exports_user_generated ON report_export_history(generated_by_user_id, generated_at);
CREATE INDEX idx_report_exports_type_format ON report_export_history(report_type, export_format);
CREATE INDEX idx_stock_transactions_site_date ON stock_transactions(site_id, transaction_date);
CREATE INDEX idx_stock_transactions_party_date ON stock_transactions(party_id, transaction_date);
CREATE INDEX idx_site_stock_balances_site_item ON site_stock_balances(site_id, item_id);
CREATE INDEX idx_issued_challans_dispatch ON issued_challans(dispatch_date);
CREATE INDEX idx_receiving_challans_receive_status ON receiving_challans(receive_date, status);
CREATE INDEX idx_billing_runs_period_status ON billing_runs(period_start, period_end, status);
CREATE INDEX idx_invoices_date_status ON invoices(invoice_date, status);
CREATE INDEX idx_payment_receipts_date_status ON payment_receipts(payment_date, status);
CREATE INDEX idx_tds_details_status ON tds_details(verification_status);

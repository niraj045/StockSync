CREATE TABLE document_number_sequences (
    document_type VARCHAR(40) NOT NULL,
    financial_year VARCHAR(9) NOT NULL,
    prefix VARCHAR(20) NOT NULL,
    last_number BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (document_type, financial_year),
    CONSTRAINT ck_document_sequence_number CHECK (last_number >= 0)
);

CREATE TABLE quotation_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_code VARCHAR(50) COLLATE utf8mb4_0900_ai_ci NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500) NULL,
    company_name VARCHAR(200) NULL,
    company_address VARCHAR(1000) NULL,
    company_gstin VARCHAR(15) NULL,
    header_text VARCHAR(2000) NULL,
    footer_text VARCHAR(2000) NULL,
    default_terms VARCHAR(4000) NULL,
    default_notes VARCHAR(2000) NULL,
    logo_attachment_id BIGINT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(50) NOT NULL,
    CONSTRAINT uk_quotation_template_code UNIQUE (template_code),
    CONSTRAINT fk_quotation_template_logo FOREIGN KEY (logo_attachment_id) REFERENCES file_attachments(id)
);

ALTER TABLE quotations DROP CHECK ck_quotations_status;
ALTER TABLE quotations DROP CHECK ck_quotations_amounts;
ALTER TABLE quotations
    ADD COLUMN quotation_template_id BIGINT NULL AFTER quotation_number,
    ADD COLUMN discount_type VARCHAR(20) NOT NULL DEFAULT 'NONE' AFTER subtotal,
    ADD COLUMN discount_value DECIMAL(19,4) NOT NULL DEFAULT 0 AFTER discount_type,
    ADD COLUMN discount_amount DECIMAL(19,2) NOT NULL DEFAULT 0 AFTER discount_value,
    ADD COLUMN taxable_amount DECIMAL(19,2) NOT NULL DEFAULT 0 AFTER discount_amount,
    ADD COLUMN cgst_rate DECIMAL(7,4) NOT NULL DEFAULT 0 AFTER taxable_amount,
    ADD COLUMN cgst_amount DECIMAL(19,2) NOT NULL DEFAULT 0 AFTER cgst_rate,
    ADD COLUMN sgst_rate DECIMAL(7,4) NOT NULL DEFAULT 0 AFTER cgst_amount,
    ADD COLUMN sgst_amount DECIMAL(19,2) NOT NULL DEFAULT 0 AFTER sgst_rate,
    ADD COLUMN igst_rate DECIMAL(7,4) NOT NULL DEFAULT 0 AFTER sgst_amount,
    ADD COLUMN igst_amount DECIMAL(19,2) NOT NULL DEFAULT 0 AFTER igst_rate,
    ADD COLUMN total_tax DECIMAL(19,2) NOT NULL DEFAULT 0 AFTER igst_amount,
    ADD COLUMN other_charge DECIMAL(19,2) NOT NULL DEFAULT 0 AFTER unloading_charge,
    ADD COLUMN round_off DECIMAL(19,2) NOT NULL DEFAULT 0 AFTER total_tax,
    ADD COLUMN security_deposit DECIMAL(19,2) NOT NULL DEFAULT 0 AFTER grand_total,
    ADD COLUMN rejection_reason VARCHAR(1000) NULL,
    ADD COLUMN sent_at TIMESTAMP(6) NULL,
    ADD COLUMN sent_by VARCHAR(50) NULL,
    ADD COLUMN approved_at TIMESTAMP(6) NULL,
    ADD COLUMN approved_by VARCHAR(50) NULL,
    ADD COLUMN rejected_at TIMESTAMP(6) NULL,
    ADD COLUMN rejected_by VARCHAR(50) NULL,
    ADD COLUMN cancelled_at TIMESTAMP(6) NULL,
    ADD COLUMN cancelled_by VARCHAR(50) NULL,
    ADD COLUMN cancellation_reason VARCHAR(1000) NULL,
    ADD CONSTRAINT fk_quotation_template FOREIGN KEY (quotation_template_id) REFERENCES quotation_templates(id),
    ADD CONSTRAINT ck_quotations_status CHECK (status IN ('DRAFT','SENT','APPROVED','REJECTED','EXPIRED','CANCELLED','CONVERTED')),
    ADD CONSTRAINT ck_quotation_discount_type CHECK (discount_type IN ('NONE','PERCENTAGE','FIXED')),
    ADD CONSTRAINT ck_quotation_amounts CHECK (
        transport_charge >= 0 AND loading_charge >= 0 AND unloading_charge >= 0 AND other_charge >= 0
        AND discount_value >= 0 AND discount_amount >= 0 AND taxable_amount >= 0
        AND cgst_rate BETWEEN 0 AND 100 AND sgst_rate BETWEEN 0 AND 100 AND igst_rate BETWEEN 0 AND 100
        AND cgst_amount >= 0 AND sgst_amount >= 0 AND igst_amount >= 0 AND total_tax >= 0
        AND subtotal >= 0 AND grand_total >= 0 AND security_deposit >= 0
    );

UPDATE quotations
SET taxable_amount = subtotal,
    cgst_rate = CASE WHEN tax_rate > 0 THEN tax_rate / 2 ELSE 0 END,
    sgst_rate = CASE WHEN tax_rate > 0 THEN tax_rate / 2 ELSE 0 END,
    cgst_amount = CASE WHEN tax_amount > 0 THEN tax_amount / 2 ELSE 0 END,
    sgst_amount = CASE WHEN tax_amount > 0 THEN tax_amount / 2 ELSE 0 END,
    total_tax = tax_amount;

ALTER TABLE quotation_items
    ADD COLUMN item_code_snapshot VARCHAR(50) NULL,
    ADD COLUMN item_name_snapshot VARCHAR(150) NULL,
    ADD COLUMN description_snapshot VARCHAR(500) NULL,
    ADD COLUMN size_snapshot VARCHAR(100) NULL,
    ADD COLUMN unit_snapshot VARCHAR(30) NULL,
    ADD COLUMN rental_type VARCHAR(40) NULL,
    ADD COLUMN area DECIMAL(19,4) NULL,
    ADD COLUMN weight DECIMAL(19,4) NULL,
    ADD COLUMN sequence_number INT NOT NULL DEFAULT 0,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE quotation_items qi
JOIN items i ON i.id = qi.item_id
JOIN quotations q ON q.id = qi.quotation_id
SET qi.item_code_snapshot = i.item_code,
    qi.item_name_snapshot = i.item_name,
    qi.size_snapshot = i.size,
    qi.unit_snapshot = i.unit,
    qi.rental_type = q.rental_type,
    qi.sequence_number = qi.id;

ALTER TABLE quotation_items
    MODIFY item_code_snapshot VARCHAR(50) NOT NULL,
    MODIFY item_name_snapshot VARCHAR(150) NOT NULL,
    MODIFY unit_snapshot VARCHAR(30) NOT NULL,
    MODIFY rental_type VARCHAR(40) NOT NULL;

CREATE INDEX idx_quotation_templates_active_name ON quotation_templates(active, name);
CREATE INDEX idx_quotations_valid_until ON quotations(valid_until);
CREATE INDEX idx_quotations_template ON quotations(quotation_template_id);


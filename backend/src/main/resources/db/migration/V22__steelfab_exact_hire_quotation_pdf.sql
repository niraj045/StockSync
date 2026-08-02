ALTER TABLE quotation_items
    ADD COLUMN required_quantity DECIMAL(19,4) NULL AFTER quantity,
    ADD COLUMN hire_months DECIMAL(9,2) NOT NULL DEFAULT 1.00 AFTER rental_rate,
    ADD COLUMN replacement_rate DECIMAL(19,2) NULL AFTER hire_months;

ALTER TABLE quotations
    ADD COLUMN exact_hire_fields_json LONGTEXT NULL AFTER notes,
    ADD COLUMN exact_pdf_attachment_id BIGINT NULL AFTER exact_hire_fields_json,
    ADD COLUMN exact_pdf_template_code VARCHAR(80) NULL AFTER exact_pdf_attachment_id,
    ADD COLUMN exact_pdf_template_version INT NULL AFTER exact_pdf_template_code,
    ADD COLUMN exact_pdf_coordinates_version INT NULL AFTER exact_pdf_template_version,
    ADD COLUMN exact_pdf_checksum_sha256 VARCHAR(64) NULL AFTER exact_pdf_coordinates_version,
    ADD COLUMN exact_pdf_finalized_at TIMESTAMP NULL AFTER exact_pdf_checksum_sha256,
    ADD COLUMN exact_pdf_finalized_by VARCHAR(50) NULL AFTER exact_pdf_finalized_at,
    ADD CONSTRAINT fk_quotations_exact_pdf_attachment
        FOREIGN KEY (exact_pdf_attachment_id) REFERENCES file_attachments(id);

INSERT INTO quotation_templates (
    template_code, name, description, company_name, company_address,
    header_text, footer_text, default_terms, default_notes, active,
    created_at, created_by, updated_at, updated_by, version
)
SELECT
    'STEELFAB_EXACT_HIRE_V1',
    'SteelFab Exact Hire Quotation & Agreement',
    'Exact five-page client quotation and hire agreement PDF',
    'Steel-Fab Scaffoldings & Engineering Private Limited',
    'Mumbai, Maharashtra',
    NULL, NULL, NULL, NULL, TRUE,
    CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'system', 0
WHERE NOT EXISTS (
    SELECT 1 FROM quotation_templates WHERE template_code = 'STEELFAB_EXACT_HIRE_V1'
);

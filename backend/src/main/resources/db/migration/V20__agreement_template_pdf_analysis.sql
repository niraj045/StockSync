ALTER TABLE agreement_templates
    ADD COLUMN analysis_status VARCHAR(30) NOT NULL DEFAULT 'NOT_ANALYZED' AFTER template_version,
    ADD COLUMN page_count INT NULL AFTER analysis_status,
    ADD COLUMN checksum_sha256 VARCHAR(64) NULL AFTER page_count,
    ADD COLUMN extracted_text LONGTEXT NULL AFTER checksum_sha256,
    ADD COLUMN detected_fields_json LONGTEXT NULL AFTER extracted_text,
    ADD CONSTRAINT ck_agreement_templates_analysis_status
        CHECK (analysis_status IN ('NOT_ANALYZED','REVIEW_REQUIRED','VALIDATED','FAILED'));

UPDATE agreement_templates
SET analysis_status='VALIDATED'
WHERE rendering_mode='NATIVE';

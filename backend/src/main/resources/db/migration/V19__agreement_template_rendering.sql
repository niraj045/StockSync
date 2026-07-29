ALTER TABLE agreement_templates
    ADD COLUMN template_code VARCHAR(80) NULL AFTER id,
    ADD COLUMN rendering_mode VARCHAR(20) NOT NULL DEFAULT 'REFERENCE' AFTER description,
    ADD COLUMN layout_key VARCHAR(100) NULL AFTER rendering_mode,
    ADD COLUMN built_in BOOLEAN NOT NULL DEFAULT FALSE AFTER layout_key,
    ADD COLUMN template_version INT NOT NULL DEFAULT 1 AFTER built_in,
    MODIFY original_filename VARCHAR(255) NULL,
    MODIFY stored_filename VARCHAR(255) NULL,
    MODIFY content_type VARCHAR(100) NULL,
    MODIFY file_size BIGINT NOT NULL DEFAULT 0,
    MODIFY storage_path VARCHAR(500) NULL,
    ADD CONSTRAINT uk_agreement_templates_code UNIQUE (template_code),
    ADD CONSTRAINT ck_agreement_templates_rendering_mode
        CHECK (rendering_mode IN ('NATIVE', 'REFERENCE'));

INSERT INTO agreement_templates (
    template_code, name, description, rendering_mode, layout_key, built_in, template_version,
    original_filename, stored_filename, content_type, file_size, storage_path, active,
    version, created_at, created_by, updated_at, updated_by
) VALUES (
    'ROCKS_LOGS_V1',
    'Rocks & Logs Agreement',
    'Native five-page-style hire agreement based on the Bandra site reference document.',
    'NATIVE',
    'rocks-logs-v1',
    TRUE,
    1,
    NULL, NULL, NULL, 0, NULL, TRUE,
    0, CURRENT_TIMESTAMP(6), 'system', CURRENT_TIMESTAMP(6), 'system'
);

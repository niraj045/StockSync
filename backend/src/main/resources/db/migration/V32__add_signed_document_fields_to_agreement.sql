ALTER TABLE agreements
ADD COLUMN signed_document_attachment_id BIGINT DEFAULT NULL,
ADD COLUMN signed_filename VARCHAR(255) DEFAULT NULL,
ADD COLUMN signed_uploaded_at DATETIME DEFAULT NULL;

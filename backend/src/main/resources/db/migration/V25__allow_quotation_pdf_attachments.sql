ALTER TABLE file_attachments DROP CHECK ck_file_attachments_entity_type;

ALTER TABLE file_attachments
    ADD CONSTRAINT ck_file_attachments_entity_type
    CHECK (entity_type IN (
        'PARTY', 'SITE', 'VENDOR', 'ITEM', 'AGREEMENT', 'INVOICE',
        'PAYMENT_RECEIPT', 'SECURITY_DEPOSIT', 'QUOTATION'
    ));

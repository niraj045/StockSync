SET FOREIGN_KEY_CHECKS=0;

DELETE FROM invoice_items WHERE invoice_id IN (
    SELECT id FROM invoices WHERE agreement_id IN (
        SELECT id FROM agreements WHERE agreement_number LIKE '%E2E%' OR quotation_id IN (
            SELECT id FROM quotations WHERE quotation_template_id IN (
                SELECT id FROM quotation_templates WHERE template_code LIKE '%E2E%' OR name LIKE '%E2E%'
            )
        )
    )
);

DELETE FROM invoices WHERE agreement_id IN (
    SELECT id FROM agreements WHERE agreement_number LIKE '%E2E%' OR quotation_id IN (
        SELECT id FROM quotations WHERE quotation_template_id IN (
            SELECT id FROM quotation_templates WHERE template_code LIKE '%E2E%' OR name LIKE '%E2E%'
        )
    )
);

DELETE FROM billing_run_segments WHERE agreement_item_id IN (
    SELECT id FROM agreement_items WHERE agreement_id IN (
        SELECT id FROM agreements WHERE agreement_number LIKE '%E2E%' OR quotation_id IN (
            SELECT id FROM quotations WHERE quotation_template_id IN (
                SELECT id FROM quotation_templates WHERE template_code LIKE '%E2E%' OR name LIKE '%E2E%'
            )
        )
    )
);

DELETE FROM agreement_items WHERE agreement_id IN (
    SELECT id FROM agreements WHERE agreement_number LIKE '%E2E%' OR quotation_id IN (
        SELECT id FROM quotations WHERE quotation_template_id IN (
            SELECT id FROM quotation_templates WHERE template_code LIKE '%E2E%' OR name LIKE '%E2E%'
        )
    )
);

DELETE FROM agreements WHERE agreement_number LIKE '%E2E%' OR quotation_id IN (
    SELECT id FROM quotations WHERE quotation_template_id IN (
        SELECT id FROM quotation_templates WHERE template_code LIKE '%E2E%' OR name LIKE '%E2E%'
    )
);

DELETE FROM quotation_items WHERE quotation_id IN (
    SELECT id FROM quotations WHERE quotation_template_id IN (
        SELECT id FROM quotation_templates WHERE template_code LIKE '%E2E%' OR name LIKE '%E2E%'
    )
);

DELETE FROM quotations WHERE quotation_template_id IN (
    SELECT id FROM quotation_templates WHERE template_code LIKE '%E2E%' OR name LIKE '%E2E%'
);

DELETE FROM quotation_templates WHERE template_code LIKE '%E2E%' OR name LIKE '%E2E%';

DELETE FROM agreement_templates WHERE template_code LIKE '%E2E%' OR name LIKE '%E2E%';

SET FOREIGN_KEY_CHECKS=1;

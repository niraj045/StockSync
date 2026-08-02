-- DESTRUCTIVE: remove all operational/client data while preserving the permanent
-- StockSync login baseline, Flyway history, roles, and SteelFab PDF template.
-- Run only after Flyway has applied V23 or later.
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE agreement_item_slabs;
TRUNCATE TABLE agreement_items;
TRUNCATE TABLE agreements;
TRUNCATE TABLE billing_run_charges;
TRUNCATE TABLE billing_run_segments;
TRUNCATE TABLE billing_runs;
TRUNCATE TABLE billing_source_allocations;
TRUNCATE TABLE damage_records;
TRUNCATE TABLE deposit_invoice_allocations;
TRUNCATE TABLE document_number_sequences;
TRUNCATE TABLE file_attachments;
TRUNCATE TABLE gst_export_config_versions;
TRUNCATE TABLE invoice_items;
TRUNCATE TABLE invoices;
TRUNCATE TABLE issued_challan_items;
TRUNCATE TABLE issued_challans;
TRUNCATE TABLE item_aliases;
TRUNCATE TABLE item_categories;
TRUNCATE TABLE item_exchange_records;
TRUNCATE TABLE items;
TRUNCATE TABLE loss_records;
TRUNCATE TABLE parties;
TRUNCATE TABLE payment_allocations;
TRUNCATE TABLE payment_receipts;
TRUNCATE TABLE purchase_items;
TRUNCATE TABLE purchases;
TRUNCATE TABLE quotation_items;
TRUNCATE TABLE quotations;
TRUNCATE TABLE receiving_challan_items;
TRUNCATE TABLE receiving_challans;
TRUNCATE TABLE report_export_history;
TRUNCATE TABLE saved_report_filters;
TRUNCATE TABLE scrap_entries;
TRUNCATE TABLE scrap_items;
TRUNCATE TABLE security_deposit_transactions;
TRUNCATE TABLE site_order_items;
TRUNCATE TABLE site_orders;
TRUNCATE TABLE site_stock_balances;
TRUNCATE TABLE site_transfer_items;
TRUNCATE TABLE site_transfers;
TRUNCATE TABLE sites;
TRUNCATE TABLE stock_adjustment_items;
TRUNCATE TABLE stock_adjustments;
TRUNCATE TABLE stock_balances;
TRUNCATE TABLE stock_import_batches;
TRUNCATE TABLE stock_import_location_mappings;
TRUNCATE TABLE stock_import_rows;
TRUNCATE TABLE stock_transactions;
TRUNCATE TABLE tds_details;
TRUNCATE TABLE user_activity_logs;
TRUNCATE TABLE vendors;

DELETE assignment
FROM user_roles assignment
JOIN users user ON user.id = assignment.user_id
WHERE LOWER(user.username) NOT IN (
    'admin', 'rohit.admin', 'karan.admin', 'vikram.admin', 'aman.operations', 'suresh.store'
);

DELETE FROM users
WHERE LOWER(username) NOT IN (
    'admin', 'rohit.admin', 'karan.admin', 'vikram.admin', 'aman.operations', 'suresh.store'
);

DELETE FROM quotation_templates
WHERE template_code <> 'STEELFAB_EXACT_HIRE_V1';

UPDATE quotation_templates
SET active = TRUE,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = 'client-baseline-reset'
WHERE template_code = 'STEELFAB_EXACT_HIRE_V1';

SET FOREIGN_KEY_CHECKS = 1;

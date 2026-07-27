# StockSync Tomorrow Handoff

## Current Project Position

The project is on the local `main` branch, tracking `origin/main`. The latest implemented scope is Phase 10: operational dashboard and local scale validation. Phases 1 through 9 are already present in the codebase, and Phase 10 adds a read-only operational dashboard plus local scale-data tooling.

Use this as the next starting point:

- Backend: `backend/`
- Frontend: `frontend/`
- Database migrations: `backend/src/main/resources/db/migration/`
- Main product docs: `docs/`
- Phase 10 docs: `docs/25_PHASE_10_DASHBOARD_AND_LOCAL_SCALE_VALIDATION.md`

Do not start production deployment work from here unless that is the next planned phase. The current codebase is still a local/dev application with MySQL 8.4 and Java 21 expectations.

## Local Runtime Notes

The easiest local startup path is still:

```bash
./start.sh
```

Expected local services:

- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8081/api/v1`
- Backend health: `http://localhost:8081/actuator/health`
- Database: MySQL 8.4

Backend validation should use Java 21. The repository uses the Maven Wrapper, so global Maven is not required.

## Database Position

Flyway owns the schema. Do not edit old migrations after they have been applied. Add the next change as a new migration with the next version number.

Current migrations run from `V1` through `V18`.

Important domain tables by area:

- Auth and audit: `roles`, `users`, `user_roles`, `user_activity_logs`
- Master data: `item_categories`, `items`, `parties`, `sites`, `vendors`, `file_attachments`
- Inventory: `stock_balances`, `site_stock_balances`, `stock_transactions`, `purchases`, `scrap_entries`, `stock_adjustments`
- Sales pipeline: `quotations`, `quotation_items`, `agreements`, `agreement_items`, `site_orders`, `site_order_items`
- Challans: `issued_challans`, `issued_challan_items`, `receiving_challans`, `receiving_challan_items`
- Exceptions and transfers: `loss_records`, `damage_records`, `item_exchange_records`, `site_transfers`, `site_transfer_items`
- Billing and invoices: `agreement_item_slabs`, `billing_runs`, `billing_run_segments`, `billing_run_charges`, `billing_source_allocations`, `invoices`, `invoice_items`
- Payments and deposits: `payment_receipts`, `payment_allocations`, `tds_details`, `security_deposit_transactions`, `deposit_invoice_allocations`
- Reports: `saved_report_filters`, `report_export_history`, `gst_export_config_versions`

Phase 10 added `V18__dashboard_scale_validation_indexes.sql` for dashboard-scale query support. It adds indexes only; it does not change existing business tables.

## Complete Application Flow Example

This is a sample full business flow and the type of database entries it creates. IDs are examples only.

1. Create master data.

Example records:

```sql
INSERT INTO item_categories (id, name, active) VALUES (1, 'Shuttering Plates', true);
INSERT INTO items (id, category_id, name, unit, active) VALUES (101, 1, 'MS Plate 3x2', 'PCS', true);
INSERT INTO parties (id, name, gstin, active) VALUES (201, 'ABC Infra Pvt Ltd', '27ABCDE1234F1Z5', true);
INSERT INTO sites (id, party_id, name, status) VALUES (301, 201, 'ABC Metro Site', 'ACTIVE');
```

2. Add godown stock.

Example records:

```sql
INSERT INTO stock_balances (item_id, quantity) VALUES (101, 500);
INSERT INTO stock_transactions (item_id, transaction_type, quantity, reference_type, reference_id)
VALUES (101, 'OPENING_STOCK', 500, 'OPENING_IMPORT', 1);
```

3. Create quotation and convert to agreement.

Example records:

```sql
INSERT INTO quotations (id, quotation_number, party_id, site_id, status)
VALUES (401, 'QT/2026-27/0001', 201, 301, 'APPROVED');

INSERT INTO quotation_items (quotation_id, item_id, quantity, monthly_rate)
VALUES (401, 101, 100, 35.00);

INSERT INTO agreements (id, agreement_number, quotation_id, party_id, site_id, status)
VALUES (501, 'AGR/2026-27/0001', 401, 201, 301, 'ACTIVE');

INSERT INTO agreement_items (agreement_id, item_id, quantity, monthly_rate)
VALUES (501, 101, 100, 35.00);
```

4. Create site order and issue challan.

Example records:

```sql
INSERT INTO site_orders (id, order_number, agreement_id, party_id, site_id, status)
VALUES (601, 'SO/2026-27/0001', 501, 201, 301, 'APPROVED');

INSERT INTO site_order_items (site_order_id, item_id, ordered_quantity)
VALUES (601, 101, 100);

INSERT INTO issued_challans (id, challan_number, site_order_id, party_id, site_id)
VALUES (701, 'IC/2026-27/0001', 601, 201, 301);

INSERT INTO issued_challan_items (issued_challan_id, item_id, quantity)
VALUES (701, 101, 100);
```

Stock movement effect:

```sql
UPDATE stock_balances SET quantity = quantity - 100 WHERE item_id = 101;
INSERT INTO site_stock_balances (site_id, item_id, quantity) VALUES (301, 101, 100);
INSERT INTO stock_transactions (item_id, site_id, transaction_type, quantity, reference_type, reference_id)
VALUES (101, 301, 'ISSUE', -100, 'ISSUED_CHALLAN', 701);
```

5. Receive partial return.

Example records:

```sql
INSERT INTO receiving_challans (id, challan_number, party_id, site_id, status)
VALUES (801, 'RC/2026-27/0001', 201, 301, 'POSTED');

INSERT INTO receiving_challan_items (receiving_challan_id, item_id, received_quantity)
VALUES (801, 101, 20);
```

Stock movement effect:

```sql
UPDATE site_stock_balances SET quantity = quantity - 20 WHERE site_id = 301 AND item_id = 101;
UPDATE stock_balances SET quantity = quantity + 20 WHERE item_id = 101;
INSERT INTO stock_transactions (item_id, site_id, transaction_type, quantity, reference_type, reference_id)
VALUES (101, 301, 'RETURN', 20, 'RECEIVING_CHALLAN', 801);
```

6. Record loss or damage when needed.

Example records:

```sql
INSERT INTO loss_records (id, site_id, party_id, item_id, quantity, status)
VALUES (901, 301, 201, 101, 2, 'APPROVED');

INSERT INTO damage_records (id, site_id, party_id, item_id, quantity, status)
VALUES (902, 301, 201, 101, 3, 'APPROVED');
```

7. Run rental billing and create invoice.

Example records:

```sql
INSERT INTO billing_runs (id, party_id, site_id, billing_period_start, billing_period_end, status)
VALUES (1001, 201, 301, '2026-07-01', '2026-07-31', 'APPROVED');

INSERT INTO billing_run_segments (billing_run_id, agreement_id, item_id, billable_quantity, billable_days)
VALUES (1001, 501, 101, 80, 31);

INSERT INTO billing_run_charges (billing_run_id, charge_type, amount)
VALUES (1001, 'RENTAL', 2800.00);

INSERT INTO invoices (id, invoice_number, billing_run_id, party_id, site_id, status, total_amount, balance_amount)
VALUES (1101, 'INV/2026-27/0001', 1001, 201, 301, 'ISSUED', 3304.00, 3304.00);

INSERT INTO invoice_items (invoice_id, item_id, description, quantity, rate, amount)
VALUES (1101, 101, 'MS Plate 3x2 rental', 80, 35.00, 2800.00);
```

Invoice creation does not change stock. Issued invoice snapshots should remain stable even if master data changes later.

8. Receive payment and allocate it.

Example records:

```sql
INSERT INTO payment_receipts (id, receipt_number, party_id, payment_date, amount, status)
VALUES (1201, 'RCPT/2026-27/0001', 201, '2026-08-05', 3304.00, 'POSTED');

INSERT INTO payment_allocations (payment_receipt_id, invoice_id, allocated_amount)
VALUES (1201, 1101, 3304.00);

UPDATE invoices SET balance_amount = 0.00, status = 'PAID' WHERE id = 1101;
```

9. Dashboard reads the result.

The Phase 10 dashboard reads aggregate data only. It should not create stock, billing, payment, TDS, or invoice records.

Main endpoint:

```http
GET /api/v1/dashboard/overview?dateFrom=2026-07-01&dateTo=2026-07-31&partyId=201&siteId=301&categoryId=1
```

Expected dashboard sections:

- Stock summary
- Movement summary
- Orders and challans
- Agreements
- Billing and invoices
- Payments and outstanding
- Exceptions
- Attention items
- Recent documents and activity

## Tomorrow Checklist

- Pull both `main` and `master` before starting work.
- Continue from `main` unless a specific branch is needed.
- Verify Java 21 before backend work.
- Let Flyway create or update schema; do not manually patch existing migrations.
- Run backend and frontend tests before pushing more code.

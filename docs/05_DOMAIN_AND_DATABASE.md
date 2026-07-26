# 5. Domain and Database Design

## 5.1 Database Principles

- Use MySQL 8.4.
- Use Flyway migrations.
- Use `BIGINT` primary keys or UUIDs consistently.
- Store money in `DECIMAL`, never floating-point.
- Store quantities and weights in `DECIMAL`.
- Store timestamps in UTC.
- Use soft delete only where legally or operationally required.
- Posted financial and stock records should not be physically deleted.
- Use status fields and reversal entries.
- Add audit columns to important tables.
- `items.minimum_stock` is nullable only so confirmed legacy-import items can preserve an unknown
  reorder threshold; normal item creation continues to require an explicit non-negative value.

Recommended audit columns:

- created_at
- created_by
- updated_at
- updated_by
- version

## 5.2 Core Tables

### Authentication

- users
- roles
- user_roles
- user_activity_logs
- user_sessions, if custom session tracking is needed

### Inventory

- item_categories
- items
- stock_balances
- stock_transactions
- purchases
- purchase_items
- scrap_entries
- scrap_items
- stock_adjustments
- stock_adjustment_items

### Parties and Sites

- parties
- party_documents
- vendors
- sites
- site_documents
- site_notes

### Quotations and Agreements

- quotation_templates
- quotations
- quotation_items
- agreement_templates
- agreements
- agreement_items

### Orders and Challans

- site_orders
- site_order_items
- issued_challans
- issued_challan_items
- received_challans
- received_challan_items
- challan_attachments

### Rental and Billing

- rental_rate_plans
- rental_rate_slabs
- invoices
- invoice_items
- additional_charges
- security_deposits
- security_deposit_transactions

### Payments and GST

- payments
- payment_allocations
- tds_entries
- gst_reports
- eway_bill_records

### Hired Material

- hire_partners
- hire_in_transactions
- hire_in_items
- hire_returns
- hire_return_items
- hire_partner_payments
- hire_invoices

### Files and Reports

- file_attachments
- generated_reports
- report_jobs, only if asynchronous generation is added later

## 5.3 Stock Ledger Model

`stock_transactions` is the permanent source of stock movement history.

Suggested fields:

- id
- item_id
- transaction_type
- transaction_date
- quantity
- weight
- direction
- source_type
- source_id
- site_id
- party_id
- notes
- created_by
- created_at
- reversal_of_transaction_id

`stock_balances` stores the current operational balance.

Suggested fields:

- item_id
- available_quantity
- issued_quantity
- hired_quantity
- lost_quantity
- scrapped_quantity
- available_weight
- version

## 5.4 Stock Balance Rule

The balance row is a performance optimization, not the audit source.

The transaction ledger must always allow recalculation.

Example:

```text
Purchase +1000
Issue -200
Receive +50
Loss -5
Available = 845
```

## 5.5 Order and Split Challan Model

`site_order_items`:

- ordered_quantity
- issued_quantity
- remaining_quantity

`issued_challan_items`:

- order_item_id
- item_id
- issued_quantity

The remaining quantity may be stored for performance but must be validated against the total posted challan quantities.

## 5.6 Receiving Model

A received item may contain:

- returned_quantity
- extra_quantity
- damaged_quantity
- lost_quantity
- exchanged_from_item_id
- exchanged_to_item_id

Use explicit fields rather than a single ambiguous quantity.

## 5.7 Money Model

Use `DECIMAL(19,2)` for currency unless paise-level calculations require more precision.

Suggested fields:

- taxable_amount
- cgst_amount
- sgst_amount
- igst_amount
- total_tax
- gross_amount
- tds_amount
- net_payable
- paid_amount
- outstanding_amount

## 5.8 Rental Slabs

`rental_rate_plans`:

- id
- name
- rental_type
- unit
- active

`rental_rate_slabs`:

- rate_plan_id
- from_day
- to_day
- rate
- sequence

## 5.9 File Attachment Model

Suggested fields:

- id
- entity_type
- entity_id
- category
- original_filename
- stored_filename
- storage_path
- mime_type
- size_bytes
- checksum
- uploaded_by
- uploaded_at

## 5.10 Important Constraints

- Unique item code
- Unique challan number per financial year
- Unique invoice number per financial year
- Unique agreement number where applicable
- Positive posted quantities
- No payment allocation above payment amount
- No challan issue above allowed remaining quantity
- No stock issue above available quantity
- No duplicate posting of the same draft

## 5.11 Implemented Schema Through Phase 4

Flyway `V3__master_data.sql` implements categories, items, parties, vendors, sites, and generic
file attachments. Master records use audit timestamps, actor snapshots, and optimistic versions.
Item codes, category names, site codes, and the applicable tax identifiers are unique.

Flyway `V4__inventory_core.sql` implements stock balances, the immutable stock transaction ledger,
purchases, scrap entries, and adjustments with line tables. Posting document numbers and
idempotency keys are unique. Quantities are positive and monetary/quantity values use `DECIMAL`.

The ledger is the audit source; `stock_balances` is an atomically maintained projection. Posting
locks only affected balance rows in ascending item order. A movement that would make available
stock negative rolls back its document, lines, ledger entries, and balance changes together.

Flyway `V5__agreements_and_orders.sql` adds quotation, agreement/template, and site-order
documents with explicit lifecycle status and item snapshots.

## 5.12 Phase 4.1 Opening Stock Schema

Flyway `V6__opening_stock_import.sql` adds:

- `stock_import_batches`: checksum-protected workbook identity, snapshot dates, lifecycle,
  corrected expected totals, posting/reversal actors, and optimistic version.
- `stock_import_rows`: exact Excel cell provenance, source values, normalized suggestion,
  item/location mappings, quantity, snapshot date, target bucket, validation state, exclusions,
  and posted ledger reference.
- `stock_import_location_mappings`: one reusable party/site mapping for each source column.
- `item_aliases`: case-insensitive source-system aliases pointing to normalized items.
- `stock_transactions.stock_bucket`, `import_batch_id`, and `import_row_id`: minimal ledger
  provenance extensions.

Flyway `V7__nullable_imported_item_minimum_stock.sql` allows `items.minimum_stock` to remain
null when a user explicitly creates an item from legacy import data that contains no reorder
threshold. The normal item API still requires an explicit non-negative threshold.

The 152 staging balance cells are unique by batch, Excel row, and Excel column. Quantities must
be positive. Workbook checksums and batch codes are unique. Item, party, site, and posted
transaction references use foreign keys. Source rows are never deleted during posting or
reversal.

`OPENING_GODOWN_BALANCE` increases `AVAILABLE`; `OPENING_SITE_BALANCE` increases `ISSUED`.
Total owned quantity is the sum of the relevant projected buckets. Reversal transactions point
to their original ledger entries and never mutate or delete them.

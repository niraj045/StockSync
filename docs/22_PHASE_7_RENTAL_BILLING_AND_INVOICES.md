# Phase 7: Rental Billing and Invoice Management

## Scope

Phase 7 adds rental billing runs and rental invoices. It deliberately excludes customer payments, payment allocation, TDS, security deposit receipts or refunds, credit notes, accounting journal entries, GST return filing, GSTR-1, GSTR-3B, E-Way Bills, dashboard redesign, production deployment, and Selenium E2E tests.

## Source of Truth

Billing uses posted operational movements, not site order quantity. A site order records requested material; an issued challan records material actually deployed. Receiving challans, site transfers, approved losses, damage records, and item exchanges reduce or change the future billable quantity.

Billing must not modify stock balances, site pending balances, stock ledger records, or payment records.

## Rental Timeline

The billing engine builds FIFO rental lots per agreement item from:

- Posted issued challans
- Posted incoming site transfers
- Posted item exchanges into the billed item
- Opening site balances where present

The lot is reduced by:

- Posted receiving challans
- Posted outgoing site transfers
- Approved manual losses
- Recorded manual damage
- Posted item exchanges out of the billed item

Partial issues and returns produce separate invoice-explainable segments. For example, 100 issued on August 1, 40 returned on August 10, and 20 returned on August 20 produces separate 100, 60, and 40 quantity spans instead of billing the full quantity for the full month.

## Date Rules

Agreements store billing date behavior:

- `billing_start_rule`: `ISSUE_DATE_INCLUDED` or `DAY_AFTER_ISSUE`
- `billing_end_rule`: `RETURN_DATE_INCLUDED` or `RETURN_DATE_EXCLUDED`

Legacy defaults are issue-date included and return-date excluded. Agreement grace days are applied from the configured billing start. Minimum billing days extend the billable span where applicable before the billing period intersection is calculated.

## Rental Models

The engine reuses the existing `RentalType` enum:

- `PER_PIECE_PER_DAY`
- `PLATE_AREA_PER_DAY`
- `SCAFFOLD_AREA_PER_DAY`
- `PLOT_AREA_PER_DAY`
- `FIXED_RATE`
- `SLAB_BASED`

Per-piece rental multiplies quantity, daily rate, and billable days. Area models use stored area snapshots. Slab rental uses ordered agreement item slabs and preserves the applied slab snapshot on the billing segment.

## Billing Runs

Billing runs are created for an agreement and a billing period. Suggested periods are based on the agreement billing cycle: weekly, monthly, or custom. Future periods, periods before agreement effective date, periods after expiry, and overlapping non-cancelled periods are blocked.

Statuses:

- `DRAFT`
- `CALCULATED`
- `FINALIZED`
- `CANCELLED`

Draft or calculated runs may be recalculated. Finalized runs are immutable for invoicing. Cancelled runs do not block future billing periods.

## Charges

Billing runs can include selected operational and recovery charges:

- Issued challan transport, loading, and unloading charges
- Receiving challan transport and handling charges
- Site transfer transport charges
- Approved loss recovery
- Damage recovery
- Repair charge where the damage record is repairable
- Manual authorized adjustment total
- Discount line

Selected source charges receive billing source allocations during finalization so they cannot be billed twice.

## Tax

Billing totals are calculated in the backend using `BigDecimal`. Rental, selected charges, adjustments, and discount are combined into taxable amount. CGST plus SGST or IGST are stored as snapshots on the billing run and invoice. The invoice preserves historical totals, rates, and snapshots.

## Invoices

One finalized billing run may generate one invoice draft. Invoice numbers use the financial-year numbering service with the `INV/YYYY-YY/0001` format. Billing run numbers use `BR/YYYY-YY/0001`.

Invoice statuses:

- `DRAFT`
- `ISSUED`
- `CANCELLED`

Draft invoices allow due date, terms, and notes edits with optimistic locking. Issued invoices are immutable. Cancellation requires a reason and audit entry.

Invoices preserve company, party, site, agreement, line, tax, and PDF snapshots. Master data changes after invoice issue do not recalculate issued invoices.

## PDF

Invoice PDFs are generated from invoice snapshots through the existing file attachment infrastructure. Downloads are authenticated. File paths are normalized and checked against the configured storage root to prevent traversal. The PDF is labelled as an invoice and does not reuse challan wording.

## Duplicate Billing Prevention

Phase 7 uses:

- Unique billing run numbers
- Unique invoice numbers
- Unique invoice per billing run
- Billing source allocation uniqueness by source type and source ID
- Overlapping period checks that ignore cancelled runs
- Status transition validation
- Optimistic locking on editable invoice drafts

Important backend errors include overlapping period, duplicate billing source, already finalized run, already invoiced run, already issued invoice, and invalid status transition conditions.

## Authorization

Backend controllers enforce role access with Spring Security:

- `ROLE_ADMIN`: full billing and invoice access, including cancellation
- `ROLE_ACCOUNTS`: billing review, finalization, invoice generation, PDF generation, and invoice issue
- `ROLE_OPERATIONS`: billing draft and calculation access, read-oriented invoice access
- `ROLE_VIEWER`: read-only billing and invoice access

The frontend hides unavailable actions based on the authenticated user's roles. Backend authorization remains authoritative.

## API

Billing run endpoints:

- `GET /api/v1/billing-runs`
- `GET /api/v1/billing-runs/{id}`
- `POST /api/v1/billing-runs`
- `PUT /api/v1/billing-runs/{id}`
- `POST /api/v1/billing-runs/{id}/calculate`
- `POST /api/v1/billing-runs/{id}/recalculate`
- `POST /api/v1/billing-runs/{id}/finalize`
- `POST /api/v1/billing-runs/{id}/cancel`
- `GET /api/v1/billing-runs/{id}/preview`
- `GET /api/v1/billing-runs/eligible-agreements`
- `GET /api/v1/billing-runs/agreement/{agreementId}/suggested-period`
- `GET /api/v1/billing-runs/{id}/available-charges`

Invoice endpoints:

- `GET /api/v1/invoices`
- `GET /api/v1/invoices/{id}`
- `POST /api/v1/invoices/from-billing-run/{billingRunId}`
- `PUT /api/v1/invoices/{id}`
- `POST /api/v1/invoices/{id}/generate-pdf`
- `POST /api/v1/invoices/{id}/issue`
- `POST /api/v1/invoices/{id}/cancel`
- `GET /api/v1/invoices/{id}/pdf`

## Database

Flyway migration `V15__rental_billing_and_invoice_management.sql` adds agreement billing date rules, agreement item slabs, operational charge columns, billing runs, billing run segments, billing run charges, source allocations, invoices, and invoice items.

Primary constraints include unique billing run number, unique invoice number, unique invoice per billing run, and unique source allocation.

## Frontend

The UI adds lazy-loaded routes:

- `/billing-runs`
- `/invoices`
- `/invoices/:id`

Billing run detail shows item-wise segments, billable days, calculation explanations, optional charge selection, discount, adjustment, tax preview, and grand total. Invoice detail shows snapshot party/site information, invoice lines, tax summary, PDF actions, status tags, and draft edit controls.

## Manual Verification

Manual verification should cover:

1. Login as ADMIN.
2. Create or select an active agreement.
3. Post issued and receiving challans with partial returns.
4. Create a billing run for a completed period.
5. Confirm separate timeline segments, billable days, rates, and totals.
6. Include one operational charge, one loss charge, and one damage or repair charge.
7. Finalize the billing run.
8. Generate invoice draft, generate PDF, and issue invoice.
9. Confirm issued invoice and PDF remain unchanged after master data edits.
10. Confirm overlapping billing periods and duplicate source charges are blocked.
11. Confirm role restrictions for OPERATIONS, ACCOUNTS, and VIEWER.
12. Confirm no stock balance, pending site balance, payment, TDS, or GST filing record is created.

## Known Limitations

Credit notes and payment-aware invoice statuses are reserved for later phases. If an invoiced loss or damage record is reversed after issue, Phase 7 preserves the issued invoice and documents the correction requirement for a future credit-note phase.

# Phase 5A — Quotations

## Rules

- Draft number: `QT/<financial-year>/<four-digit-sequence>`, assigned at creation.
- Financial year: 1 April through 31 March.
- Line amount: quantity × rate, half-up to two decimals.
- Discount applies to the line subtotal.
- Transport, loading, unloading and other charges are taxable.
- Taxable amount: subtotal − discount + charges.
- Equal CGST/SGST is used for intra-state; IGST for inter-state. They cannot coexist.
- Grand total: taxable amount + GST + round-off.
- Security deposit is displayed but excluded from grand total.
- Backend totals are authoritative.
- Quotations never reserve stock or create stock transactions.
- Party and site names are stored with item snapshots; later master-data edits do not alter finalized PDF output.

## Workflow and permissions

`DRAFT → SENT → APPROVED` or `DRAFT → SENT → REJECTED`. Rejection needs a reason. ADMIN may cancel DRAFT/SENT with a reason. ADMIN manages templates and all transitions. OPERATIONS manages drafts, sends and clones. ACCOUNTS and VIEWER are read-only with authenticated PDF download.

The canonical authority is `ROLE_ACCOUNTS`; no `ACCOUNTANT` or `ROLE_ACCOUNTANT` authority exists.

## Manual smoke test

1. Start MySQL, backend `8081`, frontend `5173`; log in as ADMIN.
2. Create an active quotation template.
3. Create a two-item quotation and verify its `QT/...` number and totals.
4. Compare stock balances before/after; they must be unchanged.
5. Send, approve and download the PDF; verify snapshot names/rates/totals.
6. Clone it; confirm a new number and DRAFT state.
7. Reject another SENT quotation with a reason.
8. Cancel another DRAFT/SENT quotation with a reason.
9. Confirm audit entries and read-only roles.
10. Restart services and confirm persistence.

Phase 5B Agreements and Phase 5C Site Orders are not included.

# 12. Implementation Roadmap

## Phase 5 split

- Phase 5A: quotations, templates, numbering, calculations and PDF.
- Phase 5B: agreements — implemented with quotation-only conversion, snapshots, controlled lifecycle, PDF attachments and role-aware UI. Site Orders remain deferred.
- Phase 5C: site orders — not started.

## Phase 0 — Discovery and Finalization

- Confirm client workflows
- Collect existing stock data
- Collect site data
- Collect agreement templates
- Collect challan samples
- Collect invoice formats
- Confirm rental models
- Confirm GST rules
- Confirm server type
- Confirm user accounts

## Phase 1 — Foundation

Status: Completed.

- Create backend project
- Create frontend project
- Configure MySQL
- Configure Flyway
- Configure authentication
- Configure base layout
- Configure Docker Compose
- Configure file storage
- Create audit framework
- Create common API error model

## Phase 2 — Master Data

Status: Completed, including authentication, user administration, and entity-linked documents.

- Categories
- Items
- Parties
- Sites
- Vendors
- Users
- Documents

## Phase 3 — Inventory Core

Status: Completed.

- Stock transaction ledger
- Stock balance
- Purchase entry
- Scrap entry
- Adjustment entry
- Stock history
- Dashboard totals
- Concurrency protection

## Phase 4 — Agreements and Orders

Status: Completed.

- Quotation
- Agreement template upload
- Agreement draft
- Agreement generation
- Order and order item
- Remaining quantity calculation

## Phase 4.1 — Client Opening Stock Import

Status: Completed.

- Controlled Apache POI parser for the client snapshot
- Checksum-protected staging batches and 152 cell-level balance rows
- Item alias, manual item, and ambiguity mapping
- Whole-column legal party/open-site mapping
- Corrected total and mixed-date dry-run reconciliation
- Transactional `AVAILABLE` and `ISSUED` opening ledger posting
- ADMIN-only compensating reversal with later-movement protection
- Audit report and role-aware import workspace

## Phase 5 — Challans

- Issued challan
- Split challan
- Posting
- Cancellation
- Receiving challan
- Partial return
- Extra return
- Damage
- Loss
- Size exchange
- PDF generation

## Phase 6 — Billing and Payments

- Rental calculation
- Rate slabs
- Additional charges
- Security deposits
- Invoices
- Payments
- TDS
- Outstanding balance

## Phase 7 — Reports

- Monthly site report
- Stock report
- Challan registers
- Payment register
- Invoice register
- Site ledger
- GST summary
- PDF and Excel exports

## Phase 8 — Payments, TDS, Security Deposits, and Outstanding

Status: Completed.

- Payment receipt drafts, posting, reversal, and receipt PDF
- Partial and multi-invoice allocations
- Advance/unallocated payment tracking and later allocation
- TDS certificate details and verification status
- Security-deposit receipts, refunds, explicit invoice adjustments, and reversals
- Invoice, party, site, and agreement outstanding summaries
- Role-aware backend APIs and frontend workspaces

## Phase 9 - Reports, GST Exports, and Business Analytics

Status: Completed.

- Role-aware report centre
- Inventory, site, party, operational, commercial, financial, and GST preparation reports
- Saved filters
- Export history
- PDF, Excel, and CSV exports
- GST sales register, tax summary, GSTR-1 preparation, and GSTR-3B summary
- Report access controls and audit logging

## Phase 10 - Production Readiness

- Security review
- Backup automation
- Restore test
- Performance test
- User acceptance test
- Production deployment
- Client training
- Handover documentation

## Recommended MVP Cut

For the fastest useful release:

1. Login
2. Dashboard
3. Categories and items
4. Parties and sites
5. Stock ledger
6. Issued challans
7. Received challans
8. Split challans
9. File uploads
10. Monthly site report
11. Payments
12. Basic GST summary
13. PDF export
14. Docker deployment
15. Backups

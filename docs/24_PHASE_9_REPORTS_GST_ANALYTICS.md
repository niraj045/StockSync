# Phase 9 Reports, GST Exports, and Analytics

Status: Completed.

Phase 9 adds a role-aware report centre for operational, commercial, financial,
and GST preparation reports. Reports are read-only and use existing stock,
challan, billing, invoice, payment, TDS, and security-deposit records as their
source of truth.

## Migration

`V17__reports_gst_analytics.sql` adds saved report filters, report export
history, GST export configuration versions, and report query indexes. Earlier
migrations are unchanged.

## Report Catalogue

- Inventory: current stock, godown stock, site pending stock, item ledger, low stock, damage/loss/scrap
- Site and party: party stock, site stock, agreements, outstanding, monthly site statement
- Operational: site orders, issued challans, receiving challans, transfers, losses, damages, exchanges, purchases, scrap, stock adjustments
- Commercial: quotations, agreements, billing runs, invoices
- Financial: payments, TDS, security deposits, outstanding ageing, party/site ledger
- GST preparation: sales register, tax summary, GSTR-1 preparation, GSTR-3B summary

## APIs

Reports are exposed under `/api/v1/reports`:

- `GET /catalog`
- `POST /{reportType}/preview`
- `POST /{reportType}/export`
- `GET /exports`
- `GET /exports/{id}/download`
- `GET|POST|PUT|DELETE /saved-filters`
- `GET /outstanding/ageing`
- `GET /sites/{siteId}/monthly-statement`
- `GET /gst/sales-summary`
- `POST /gst/gstr1-export`
- `POST /gst/gstr3b-summary`

## GST Preparation

GST reports prepare data for review and export only. They do not file returns,
store GST portal credentials, generate IRNs, create e-way bills, or create credit
notes. GST export rows include validation status and warnings for missing or
invalid GST fields.

## Exports

Supported formats are CSV, Excel, and PDF. Every export writes an export-history
row with status, format, filename, size, user, and storage path. Downloads
require the original exporting user or an administrator.

## Authorization

Reports are filtered by canonical application roles. Administrators can access
all reports. Accounts users can access financial and GST reports. Operations
users can access inventory, site, party, and operational reports. Viewers are
limited to non-sensitive read-only reports.

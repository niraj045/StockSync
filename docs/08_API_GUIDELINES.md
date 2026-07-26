# 8. API Guidelines

## 8.1 Base Path

```text
/api/v1
```

## 8.2 Main Resources

```text
/api/v1/auth
/api/v1/users
/api/v1/dashboard
/api/v1/categories
/api/v1/items
/api/v1/stock
/api/v1/parties
/api/v1/sites
/api/v1/quotations
/api/v1/agreements
/api/v1/orders
/api/v1/issued-challans
/api/v1/received-challans
/api/v1/invoices
/api/v1/payments
/api/v1/security-deposits
/api/v1/reports
/api/v1/files
/api/v1/audit-logs
```

## 8.3 API Style

- Use nouns for resources.
- Use POST for creation.
- Use PUT or PATCH for draft updates.
- Use explicit action endpoints for state changes.
- Use pagination for lists.
- Use consistent filters.
- Return DTOs, not entities.

## 8.4 State Action Examples

```text
POST /api/v1/issued-challans/{id}/post
POST /api/v1/issued-challans/{id}/cancel
POST /api/v1/invoices/{id}/issue
POST /api/v1/payments/{id}/reverse
POST /api/v1/agreements/{id}/generate
```

## 8.5 Pagination Response

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

## 8.6 Error Response

```json
{
  "timestamp": "2026-07-25T18:30:00Z",
  "status": 409,
  "code": "INSUFFICIENT_STOCK",
  "message": "Available quantity is lower than requested quantity",
  "fieldErrors": []
}
```

## 8.7 Idempotency

Posting stock or payment operations should accept an idempotency key when practical.

Example header:

```text
Idempotency-Key: 9b8a...
```

This prevents duplicate posting if the browser retries.

## 8.8 File Upload

Use multipart form data.

Validate:

- MIME type
- Extension
- File size
- Business entity
- Upload permission

## 8.9 Report Endpoints

Examples:

```text
GET /api/v1/reports/sites/{siteId}/monthly?month=2026-07&format=pdf
GET /api/v1/reports/stock?from=2026-07-01&to=2026-07-31&format=xlsx
GET /api/v1/reports/gst/gstr1?financialYear=2026-27&format=json
```

## 8.10 OpenAPI

Expose Swagger UI only to authorized users in production or disable public access.

Document:

- Request fields
- Validation rules
- Response examples
- Error codes
- Required roles

## 8.11 Implemented Phase 2 through Phase 4 Endpoints

Master data:

```text
GET/POST/PUT /api/v1/categories
GET/POST/PUT /api/v1/items
GET/POST/PUT /api/v1/parties
GET/POST/PUT /api/v1/sites
GET/POST/PUT /api/v1/vendors
GET/POST /api/v1/files
GET /api/v1/files/{id}/download
```

Inventory:

```text
GET  /api/v1/stock/balances
GET  /api/v1/stock/transactions
GET  /api/v1/stock/summary
POST /api/v1/stock/purchases
POST /api/v1/stock/scrap
POST /api/v1/stock/adjustments
```

All endpoints require an authenticated session. Master-data writes and file uploads require
`ROLE_ADMIN`. Inventory posting requires `ROLE_ADMIN` or `ROLE_OPERATIONS`; reads also allow
`ROLE_VIEWER`. Every inventory POST requires an `Idempotency-Key` header.

Commercial workflow:

```text
GET/POST/PUT /api/v1/quotations
POST /api/v1/quotations/{id}/clone|send|approve|reject|expire
POST /api/v1/quotations/{id}/convert
GET/POST/PUT /api/v1/agreements
POST /api/v1/agreements/{id}/generate|activate|terminate
GET  /api/v1/agreements/{id}/document
GET/POST /api/v1/agreement-templates
GET /api/v1/agreement-templates/{id}/download
GET/POST/PUT /api/v1/orders
POST /api/v1/orders/{id}/confirm|cancel
```

Quotation and agreement edits are limited to drafts. Conversion requires an approved quotation
and is atomic. Agreement activation requires a generated document. Order confirmation atomically
enforces the agreement item allocation ceiling. An order line's `remainingQuantity` is
`orderedQuantity - issuedQuantity`; Phase 5 will own issued-quantity updates.

Commercial reads allow every authenticated role. Writes require `ROLE_ADMIN` or
`ROLE_OPERATIONS`; template upload and agreement termination require `ROLE_ADMIN`.

## 8.12 Phase 4.1 Opening Stock Import API

```text
POST /api/v1/stock-imports/upload
GET  /api/v1/stock-imports
GET  /api/v1/stock-imports/{id}
GET  /api/v1/stock-imports/{id}/rows
PUT  /api/v1/stock-imports/{id}/rows/{rowId}/item-mapping
GET  /api/v1/stock-imports/{id}/location-mappings
PUT  /api/v1/stock-imports/{id}/location-mappings
POST /api/v1/stock-imports/{id}/validate
GET  /api/v1/stock-imports/{id}/preview
POST /api/v1/stock-imports/{id}/post
POST /api/v1/stock-imports/{id}/reverse
GET  /api/v1/stock-imports/{id}/report
```

Rows are paginated. Every authenticated role may read batches, rows, mappings, previews, and
reports. ADMIN and OPERATIONS may upload, map, and validate. Only ADMIN may post or reverse.

Posting requires `{ "confirmed": true, "expectedChecksum": "<64 hex characters>" }`. Reversal
requires a nonblank reason. Important errors include `IMPORT_FILE_ALREADY_EXISTS`,
`IMPORT_NOT_VALIDATED`, `IMPORT_TOTALS_UNEXPLAINED`, `IMPORT_ALREADY_POSTED`,
`IMPORT_ALREADY_REVERSED`, and `IMPORT_REVERSAL_UNSAFE`.

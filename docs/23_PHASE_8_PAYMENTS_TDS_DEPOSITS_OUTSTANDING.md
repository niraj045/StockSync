# Phase 8 Payments, TDS, Deposits, and Outstanding

Phase 8 adds settlement management after invoice issue. It does not add credit notes,
accounting journals, GST filing, GSTR returns, bank reconciliation, or dashboard redesign.

Payment receipts are numbered as `PR/YYYY-YY/0001`. A receipt may contain cash, TDS, or both.
Draft receipts can be edited; posting makes the receipt immutable. One receipt can allocate to
multiple issued invoices, and one invoice can receive multiple posted receipts.

Invoice outstanding is:

```text
invoice grand total - posted cash allocations - posted TDS allocations - posted deposit adjustments
```

Cash is actual money received. TDS reduces invoice outstanding as settlement but is never
counted as cash. TDS certificate details and verification status can be maintained after receipt
creation without changing the original amount.

Unallocated posted receipt value is retained as party advance and can later be allocated through
`POST /api/v1/payments/{id}/allocate`.

Security deposits are separate from normal payments and are numbered as `SD/YYYY-YY/0001`.
Receipt increases available deposit; refund and explicit invoice adjustment decrease available
deposit. Deposits are never auto-applied to invoices.

Deposit available balance is:

```text
received - adjusted - refunded
```

Posted financial records are not deleted. Payment reversal restores invoice cash/TDS settlement
totals and removes remaining advance from that receipt. Deposit adjustment reversal restores
invoice outstanding. Only `ROLE_ADMIN` can reverse posted financial records.

## Permissions

- `ROLE_ADMIN`: full payment, TDS, deposit, receipt PDF, and reversal access.
- `ROLE_ACCOUNTS`: create/edit/post payments, allocate advance, maintain/verify TDS, record
  deposits, refund deposits, adjust deposits, and download receipts.
- `ROLE_OPERATIONS`: read summaries and create payment drafts.
- `ROLE_VIEWER`: read-only access.

## APIs

```text
GET/POST/PUT /api/v1/payments
POST /api/v1/payments/{id}/post
POST /api/v1/payments/{id}/allocate
POST /api/v1/payments/{id}/reverse
GET  /api/v1/payments/{id}/receipt
GET  /api/v1/payments/party/{partyId}/eligible-invoices
GET  /api/v1/payments/party/{partyId}/available-advance
PUT  /api/v1/payments/{id}/tds-details
POST /api/v1/payments/{id}/tds/verify
POST /api/v1/payments/{id}/tds/reject

GET  /api/v1/security-deposits
GET  /api/v1/security-deposits/{id}
POST /api/v1/security-deposits/receipt
POST /api/v1/security-deposits/refund
POST /api/v1/security-deposits/adjust-to-invoice
POST /api/v1/security-deposits/{id}/reverse
GET  /api/v1/security-deposits/agreement/{agreementId}/summary

GET /api/v1/outstanding/invoices/{invoiceId}
GET /api/v1/outstanding/sites/{siteId}
GET /api/v1/outstanding/parties/{partyId}
GET /api/v1/outstanding/agreements/{agreementId}
```

## Receipt PDF

Posted receipts can generate an authenticated PDF. The PDF separates cash and TDS, shows invoice
allocations and unallocated advance, and is stored as a file attachment so repeated downloads
remain stable.

## Known Limitations

- No credit notes.
- No accounting ledger or journal postings.
- No bank reconciliation.
- No GST filing or return export.
- No automatic deposit settlement.

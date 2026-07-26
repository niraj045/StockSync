# 2. Product Requirements

## Phase 5A — quotation management

StockSync supports reusable quotation templates and financial-year numbered quotations. Item identity, unit, size, rate and commercial values are stored as historical snapshots. The workflow is `DRAFT → SENT → APPROVED` or `DRAFT → SENT → REJECTED`; ADMIN may cancel a draft or sent quotation. Quotations never reserve inventory or create ledger entries.

## 2.1 Confirmed Core Requirements

### Dashboard

The dashboard must clearly display:

- Total Inward
- Total Outward
- Current Available Stock

Recommended additional dashboard cards:

- Issued Stock
- Hired Stock
- Lost Stock
- Scrapped Stock
- Active Sites
- Defaulter Sites
- Pending Payments
- Current Month Billing
- Current Month Payments

### Contracts and Agreements

The system must:

- Manage site-specific contracts
- Store standard agreement formats
- Allow users to upload predefined agreement formats
- Read supported agreement data such as items, quantities, rates, and prices
- Generate an editable draft
- Store final agreements against the relevant site
- Generate downloadable documents

### Issued Challans

The system must:

- Create issued or delivery challans
- Support partial dispatch
- Split one large order into multiple challans
- Track ordered, dispatched, and remaining quantities
- Generate automatic challan numbers
- Reduce stock automatically
- Generate PDF challans

Example:

- Order: 1,000 units
- Challan 1: 200 units
- Challan 2: 500 units
- Challan 3: 300 units

### Received Challans

The system must:

- Allow manual entry of received challan details
- Upload challan files or photos
- Clearly display uploaded documents
- Support partial returns
- Support extra quantities
- Support size exchange
- Track damaged and lost quantities
- Update stock automatically

### Monthly Reports

The system must:

- Calculate site-wise monthly totals
- Show exact issued quantities
- Show exact received quantities
- Show pending quantities
- Show billing totals
- Show payment totals
- Show outstanding balances
- Export reports as PDF

### GST and Payments

The system must:

- Record payments
- Support partial payments
- Support TDS
- Show outstanding balance
- Generate GST summaries and reports

## 2.2 Extended Product Requirements

The following features are part of the broader product vision and should be implemented by priority, not assumed to be part of the smallest delivery.

### Inventory and Stock

- Categories
- Items
- Item sizes
- Units
- Item weight
- Item values
- Available status
- Issued status
- Lost status
- Hired status
- Scrapped status
- Item stock history
- Purchase entries
- Scrap entries
- Adjustments
- Stock transfers
- Date-range reports

### Rental Models

- Per piece per day
- Plate area based
- Scaffold area based
- Plot area based
- Flexible rate slabs
- First period rate
- Subsequent period rate
- Site-specific rates
- Item-specific rates

### Site and Party

- Party management
- GSTIN
- Site management
- Documents
- Photos
- Notes
- Defaulter status
- Closed status
- No-activity filters
- No-payment-activity filters
- Party-wise and site-wise ledgers

### Quotations

- Dynamic quotations
- Configurable formats
- Clone quotation
- Convert quotation to agreement
- PDF generation
- Version tracking

### Hired Material

- Hire in from partner
- Return to partner
- Partner payment tracking
- Hire invoices
- Automatic stock changes

### Additional Charges

- Transport
- Loading
- Unloading
- Labour
- Other custom charges

### Security Deposits

- Record deposit
- Refund deposit
- Keep deposits separate from payments
- Deposit history

### Loss and Damage

- Piece-based loss charge
- Weight-based loss charge
- Damage charge
- Site-wise recovery

### Billing

- Weekly invoices
- Monthly invoices
- Bulk invoices
- Multiple invoice formats
- Missing invoice detection
- Financial-year-based invoice numbering
- Proforma invoice
- Date-range invoice

### Compliance

- GSTR-1 PDF
- GSTR-1 Excel
- GSTR-1 JSON
- GSTR-3B JSON
- E-Way Bill generation
- E-Way Bill cancellation

### Operations

- Daily register
- Site-to-site transfer
- Activity logs
- Daily backups
- Multiple simultaneous users
- Multi-company support
- Multi-branch support

## 2.3 Requirement Priorities

### P0 — Mandatory Foundation

- Login
- Dashboard core metrics
- Categories and items
- Stock ledger
- Parties and sites
- Issued challans
- Received challans
- Split challans
- Agreements
- File uploads
- Monthly site report
- Payments
- Outstanding balance
- Basic GST summary
- PDF export
- Audit log
- Backup

### P1 — Important Business Features

- Quotations
- Rental calculation
- Extra charges
- Security deposits
- Loss and damage
- Purchase and scrap reports
- TDS
- Invoice generation
- Site ledger
- Excel exports

### P2 — Advanced Features

- Hired material from partner
- Bulk invoicing
- Missing invoice detection
- GSTR JSON generation
- E-Way Bills
- Multi-company
- Multi-branch
- Advanced filters
- OCR or intelligent agreement reading

## 2.4 Open Business Questions

Before final implementation, confirm:

1. Which rental calculation models are mandatory in version one?
2. Is billing calculated from issued date until received date?
3. How are partial returns billed?
4. How are exchanged sizes priced?
5. Can an agreement rate change after issue?
6. Are transport and labour charges taxable?
7. Is TDS deducted per payment or per invoice?
8. Is GST calculated per item or invoice total?
9. What exact invoice formats are required?
10. What exact agreement file formats will be uploaded?
11. Are scanned PDFs expected?
12. Is one godown sufficient initially?
13. Will users have separate accounts?
14. Is multi-company required now or later?

## 2.5 Client Opening Stock Migration

The client legacy snapshot is imported through a controlled staging workflow, not by replacing
normalized Item, Party, Site, or Stock models. The supported source format is
`STEELFAB_STOCK_SNAPSHOT_V1`.

- Preserve 42 exact source item rows and all 16 source party/site labels.
- Read party/site quantities from C:R and godown quantities from U.
- Ignore source totals T and V. T omits P:R and understates party/site stock by 2,418.
- Recalculate party/site stock as 20,637, godown stock as 25,382, and combined owned stock as
  46,019.
- Warn that party/site stock is dated 25-07-2026 while godown stock is dated 16-07-2026.
- Require explicit item decisions for duplicates, damaged-condition wording, variants, and
  shorthand names.
- Require a legal party and an open site mapping for every source party/site column.
- Show expected, mapped, excluded, unresolved, and posted totals before posting.
- Post immutable opening-balance ledger entries transactionally and idempotently.
- Never create purchases, vendors, orders, challans, or historical rental charges from the
  opening snapshot.
- Reverse only through ADMIN-authorized compensating transactions with a required reason.

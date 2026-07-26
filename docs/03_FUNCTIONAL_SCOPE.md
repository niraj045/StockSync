# 3. Functional Scope

## Phase 5A boundary

Included: quotation templates, draft authoring, calculations, send/approve/reject/cancel, clone, filters and PDF. Agreements, agreement templates, site orders, challans, reservations, invoices, payments and reporting are excluded. Prototype agreement/order navigation is hidden until those phases.

## 3.1 Authentication and Users

### Features

- Login
- Logout
- Change password
- Create user
- Deactivate user
- Reset password
- Assign permission set
- Track user actions

### Recommended Initial Roles

- ADMIN
- OPERATIONS
- ACCOUNTS
- VIEWER

All users may initially receive ADMIN permissions if the client prefers simple access.

## 3.2 Dashboard

### Core Metrics

- Total inward quantity
- Total outward quantity
- Current available quantity

### Additional Metrics

- Issued quantity
- Hired quantity
- Lost quantity
- Scrapped quantity
- Active sites
- Closed sites
- Defaulter sites
- Outstanding amount
- Current month billing
- Current month payments

### Dashboard Filters

- Date range
- Financial year
- Category
- Item
- Party
- Site

## 3.3 Item and Category Management

### Category Fields

- Name
- Description
- Active status

### Item Fields

- Item code
- Item name
- Category
- Size
- Unit
- Weight per piece
- Purchase value
- Rental configuration
- Loss rate
- Scrap value
- Minimum stock
- Active status

## 3.4 Stock Management

### Transaction Types

- PURCHASE
- ISSUE
- RECEIVE
- LOSS
- DAMAGE
- SCRAP
- ADJUSTMENT_IN
- ADJUSTMENT_OUT
- HIRE_IN
- HIRE_RETURN
- SITE_TRANSFER_OUT
- SITE_TRANSFER_IN

### Rules

- Every stock change must create a stock transaction.
- Stock balance must not be edited directly.
- Every transaction must reference its source.
- Transactions must be immutable after posting.
- Corrections must be handled through reversal or adjustment entries.
- Negative stock should be blocked unless explicitly authorized.
- Concurrent issue requests must not oversell available stock.

## 3.5 Party and Site Management

### Party

- Legal name
- Trade name
- GSTIN
- PAN
- Contact person
- Phone
- Email
- Address
- State
- Documents
- Notes
- Active status

### Site

- Party
- Site name
- Site code
- Address
- Contact person
- Start date
- Expected end date
- Status
- Defaulter flag
- Closed date
- Notes
- Documents
- Photos

### Site Status

- ACTIVE
- ON_HOLD
- DEFAULTER
- CLOSED

## 3.6 Quotations and Agreements

### Quotation

- Party
- Site
- Validity
- Items
- Quantities
- Rates
- Rental type
- Charges
- Taxes
- Terms
- Notes
- Status

### Quotation Status

- DRAFT
- SENT
- APPROVED
- REJECTED
- EXPIRED
- CONVERTED

### Agreement

- Agreement number
- Site
- Effective date
- Expiry date
- Rental terms
- Item rates
- Charges
- Security deposit
- Uploaded template
- Generated draft
- Final document
- Status

## 3.7 Orders and Split Challans

An order or agreement requirement represents the total intended quantity.

Issued challans represent actual dispatches.

### Order Rules

- An order item has ordered quantity.
- Issued quantity is the sum of posted issued challans.
- Remaining quantity is ordered minus issued.
- Issued quantity must not exceed ordered quantity unless override is allowed.
- Each challan may dispatch only part of the order.
- Challans may be created on different dates.

## 3.8 Issued Challans

### Fields

- Challan number
- Party
- Site
- Agreement
- Order
- Issue date
- Vehicle number
- Driver details
- Transport details
- Items
- Quantities
- Weight
- Charges
- Notes
- Attachments
- Status

### Status

- DRAFT
- POSTED
- CANCELLED

Posting a challan:

1. Validates site and agreement.
2. Validates remaining order quantity.
3. Validates available stock.
4. Creates challan and items.
5. Creates stock OUT transactions.
6. Updates site pending quantity.
7. Creates audit entry.

## 3.9 Receiving Challans

### Fields

- Receiving number
- Site
- Linked issued challan, if applicable
- Receive date
- Items
- Returned quantity
- Extra quantity
- Damaged quantity
- Lost quantity
- Exchanged item
- Notes
- Attachments
- Status

### Rules

- Received quantity increases godown stock.
- Damaged or lost quantity does not increase usable stock.
- Extra returned quantity must be explicitly identified.
- Size exchange must create an OUT transaction for the returned original accounting and an IN transaction for the actual item received.
- Pending site quantity must be recalculated.

## 3.10 Rental Calculation

### Supported Models

- PER_PIECE_PER_DAY
- PLATE_AREA_PER_DAY
- SCAFFOLD_AREA_PER_DAY
- PLOT_AREA_PER_DAY
- FIXED_RATE
- SLAB_BASED

### Slab Example

- Day 1–30: rate A
- Day 31–45: rate B
- Day 46 onward: rate C

### Calculation Inputs

- Issue date
- Receive date or billing date
- Quantity
- Unit
- Area
- Rate
- Slab
- Grace period
- Minimum billable period
- Agreement override

## 3.11 Billing and Invoices

### Invoice Types

- Weekly
- Monthly
- Date range
- Bulk
- Proforma

### Invoice Inputs

- Rental charges
- Transport
- Loading
- Unloading
- Labour
- Loss
- Damage
- Other charges
- Discounts
- GST
- TDS information
- Security deposit adjustment, if allowed

### Invoice Status

- DRAFT
- ISSUED
- PARTIALLY_PAID
- PAID
- CANCELLED

## 3.12 Payments

### Payment Fields

- Party
- Site
- Invoice
- Payment date
- Amount
- Mode
- Reference number
- TDS
- Notes
- Attachment

### Rules

- A payment may apply to one or more invoices.
- Partial payments are allowed.
- Security deposits remain separate.
- Outstanding balance must be calculated automatically.
- Cancelled payments must be reversed, not deleted.

## 3.13 Reports

### Site Reports

- All transactions
- Issued
- Received
- Pending items
- Pending weight
- Payments
- Bills
- Ledger
- Monthly site report

### Business Reports

- Party-wise pending items
- Party-wise ledger
- Stock status
- Stock history
- Purchase register
- Scrap register
- Invoice register
- Payment register
- Challan register
- Monthly billing
- Billing vs payment
- GST summary

## 3.14 Files and Documents

Supported initial formats:

- PDF
- DOCX
- XLSX
- JPG
- JPEG
- PNG

The system must:

- Validate file type
- Validate size
- Store original filename
- Generate unique stored filename
- Record uploader and timestamp
- Link files to business entities
- Prevent direct public access

## 3.15 Opening Stock Import

Phase 4.1 provides a dedicated legacy migration workspace:

1. Upload and checksum the controlled XLSX workbook.
2. Parse `Sheet1` with Apache POI into staging rows.
3. Review source formula and mixed-date warnings.
4. Map each source item to an alias, existing item, newly confirmed item, or documented
   exclusion.
5. Map each of the 16 source location columns to a legal party and open site.
6. Validate and reconcile expected, mapped, excluded, error, and posted totals.
7. Require explicit ADMIN confirmation and matching checksum.
8. Post all opening balances in one transaction.
9. Retain an import audit report and immutable reversal history.

Godown rows create `OPENING_GODOWN_BALANCE` movements into `AVAILABLE`. Party/site rows create
`OPENING_SITE_BALANCE` movements into `ISSUED`. These represent the opening state only; they do
not reconstruct historical purchases, dispatches, or rental periods.

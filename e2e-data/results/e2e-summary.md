# StockSync local E2E result

Final verdict: **READY_WITH_MINOR_ISSUES**

## Environment

- Workspace: local StockSync checkout
- Java: 21.0.11
- Maven: 3.9.11
- Node: 22.11.0
- MySQL: isolated 8.0.46 on `127.0.0.1:3009`
- Client E2E database: `shuttering_inventory_e2e`
- Scale database: `shuttering_inventory_e2e_large`
- Backend: `127.0.0.1:8082` because unrelated local software owns 8081
- Frontend: `127.0.0.1:5173`
- Local storage: `e2e-data`

## Client workbook and opening stock

The unmodified client workbook passed. It produced exactly 42 source items and
152 non-zero balances: party/site 20,637, godown 25,382, combined 46,019.
Posting created 152 ledger rows and no quotation, agreement, order, or challan.
Commercial fields absent in the workbook remained null. Duplicate upload and
post were rejected. A second controlled batch posted and reversed with 152
original plus 152 compensating rows; repost after reversal was rejected.

## Real business flow

ADMIN completed quotation, approval, PDF, agreement generation and activation,
two-stage order fulfilment, issued challans, receiving with good/damaged/lost
quantities, repair, scrap, site transfer, rental billing, loss/damage charges,
invoice, cash, TDS, deposit adjustment, payment reversal, outstanding, PDFs,
reports, GST exports, and export-history download.

Source invoice total was 102,471. Cash 40,000, TDS 2,000, and deposit adjustment
5,000 reconcile to outstanding 55,471.

## Scale and performance

The deterministic LARGE profile generated 1,200 items, 180 parties, 540 sites,
1,200 agreements, 5,000 orders, 10,000 billing runs/invoices, 5,000 payments,
100,000 stock transactions, and 2,000 each loss/damage records.

Across three authenticated calls, warm local timings were approximately:

- Dashboard: 407–479 ms
- Stock page: 49–88 ms
- Ledger page: 63–65 ms
- Invoice page: 87–127 ms
- Outstanding ageing: 147–178 ms
- GST report: 277–431 ms

## Reconciliation and authorization

Inventory database projections, dashboard, and reports match: available 24,952;
site pending 21,043; damaged 11; lost 11; scrapped 2. Financial outstanding
matches invoice, party/site reports, and ageing at 55,471.

ADMIN has full access. OPERATIONS can create operational records but cannot
reverse payments or administer users. ACCOUNTS can enter billing workflows but
cannot create orders. VIEWER mutations are forbidden. All probes matched the
intended role policy.

## Restart and automated checks

Backend and frontend were stopped and restarted. Login, dashboard, invoice,
invoice PDF, and stock report all returned HTTP 200 through the frontend proxy;
stored data and generated files remained accessible.

- Backend: 70 tests passed; Maven package passed; Flyway validated 18 migrations.
- Frontend: 50 tests passed; production build passed.

## Defects

Fixed and rerun: opening-site projections, receiving damaged projections,
receiving PDF rendering, linked billing charges, report SQL, frontend test
dependency alignment, test cleanup ordering, and scale shutdown ordering.

Remaining minor issues:

- Unknown damage enum values return 500 instead of structured 400.
- Local MySQL is 8.0.46 rather than requested 8.4.
- Node 22.11 is slightly below Vite's supported 22.12 floor, although tests and
  build pass.

No production deployment, HTTPS, Nginx, client handover, or training was done.

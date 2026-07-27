# Phase 10 Operational Dashboard and Local Scale Validation

Status: Completed.

Phase 10 adds a single operational dashboard at `/` backed by
`GET /api/v1/dashboard/overview`. The dashboard is read-only and uses live
source tables: stock balances, site stock balances, stock transactions, site
orders, challans, agreements, billing runs, invoices, payment receipts, TDS,
security deposits, loss records, damage records, reports/export metadata, and
audit activity.

## Dashboard Metrics

- Stock: godown available, material at sites, damaged, lost, scrapped, under repair, physical current stock, accountable stock
- Movements: issued today, received today, issued in period, received in period, site transfers in period
- Operations: open orders, partially fulfilled orders, fulfilled orders, draft returns, extra-return approvals
- Commercial: active agreements, expiring agreements, draft billing runs, draft invoices, issued invoices
- Finance: period invoiced, cash received, TDS, deposit adjustments, outstanding, advances, available deposits
- Exceptions: losses awaiting approval, damage awaiting action, repair material, low stock, overdue invoices, high outstanding parties

Physical current stock is `godown available + material at sites + damaged`.
Lost and scrapped quantities are shown separately and are not counted as usable
stock. Accountable stock includes lost stock separately from physical stock.

## Filters

The endpoint accepts:

- `partyId`
- `siteId`
- `categoryId`
- `dateFrom`
- `dateTo`

The frontend exposes the date range plus party, site, and category ID filters.
Backend filters are applied in aggregate SQL and do not load full transaction
tables into memory.

## Role Behavior

- `ROLE_ADMIN`: all dashboard sections and all quick actions
- `ROLE_OPERATIONS`: operational view and operational quick actions
- `ROLE_ACCOUNTS`: financial sections and accounts quick actions; operational overview remains read-only
- `ROLE_VIEWER`: read-only dashboard with no quick actions and no financial totals

Backend and frontend both derive behavior from canonical roles only.

## Attention Items

Attention items are generated for low stock, partially fulfilled orders, overdue
invoices, damaged material, pending loss approvals, expiring agreements, high
outstanding parties, and draft billing runs. Each item includes a target path for
navigation to the relevant operational page.

## Charts

Charts are limited to:

- Stock by status
- Issued versus received trend
- Top sites by pending material
- Outstanding ageing

The dashboard route is lazy-loaded so chart dependencies do not increase the
initial application bundle.

## Synthetic Data Generator

`LocalScaleDataGenerator` is available only under the `local-scale` Spring
profile and runs only when explicitly enabled:

```powershell
cd C:\stockSync\backend
$env:SPRING_PROFILES_ACTIVE = 'local-scale'
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--stocksync.scale.generator.enabled=true --stocksync.scale.profile=SMALL --stocksync.scale.seed=20260728"
```

Add `--stocksync.scale.generator.exit=true` to make the process print timings
and stop after generation during local validation.

Profiles:

- `SMALL`: 10 parties, 20 sites, 80 items, 80 agreements, 200 orders, 300 invoices, 2,500 stock transactions
- `MEDIUM`: 60 parties, 180 sites, 350 items, 350 agreements, 1,500 orders, 2,500 invoices, 25,000 stock transactions
- `LARGE`: 180 parties, 540 sites, 1,200 items, 1,200 agreements, 5,000 orders, 10,000 invoices, 100,000 stock transactions

The generator uses deterministic seeded data, valid foreign keys, mixed statuses,
low-stock items, active/expired/terminated agreements, partial orders, payments,
TDS, advances, deposits, losses, damages, overdue invoices, and ageing buckets.
It must be run only against a separate local database.

## Index Changes

`V18__dashboard_scale_validation_indexes.sql` adds targeted indexes for
dashboard and high-volume list filters:

- `site_orders(status, order_date)`
- `issued_challans(site_order_id, dispatch_date)`
- `receiving_challans(site_id, status, receive_date)`
- `invoices(due_date, status)`
- `payment_receipts(status, payment_date)`

No previous migration was modified.

## Performance Results

Local validation used MySQL 8.4 fallback on `127.0.0.1:3307` with Java 21.

Measured results are recorded in the Phase 10 completion report for the current
machine. Targets are guidance rather than fake pass/fail gates.

## Known Limitations

- PDF export size limits remain practical rather than hard-coded by dashboard code.
- Large profile generation is intentionally bounded to avoid local memory exhaustion.

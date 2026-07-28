# Local E2E, real-data, and scale testing

## Purpose

This run validates StockSync locally with the actual client stock workbook, a
complete API-driven business workflow, independent database reconciliation,
role authorization, document/report generation, restart persistence, and a
deterministic 100,000-row ledger dataset.

## Isolation

Use the `e2e` Spring profile. It points to MySQL port 3009 and refuses startup
unless the JDBC catalog equals `stocksync.e2e.expected-database`. Client E2E
files live under `e2e-data`; the scale run uses a separate database so generated
rows cannot alter the real workbook reconciliation.

Example backend startup:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 \
E2E_DB_PORT=3009 E2E_BACKEND_PORT=8082 E2E_STORAGE_PATH=../e2e-data \
mvn spring-boot:run -Dspring-boot.run.profiles=e2e
```

Example frontend startup:

```bash
VITE_API_PROXY_TARGET=http://127.0.0.1:8082 npm run dev -- --host 127.0.0.1
```

Port 3006 is not used. In this run an unrelated process owned backend port 8081,
so the isolated backend used 8082.

## Workbook validation

The source is `client-data/original/Stock of material as on 25-07-26.xlsx`.
Never edit it. Upload, map items and all C:R locations, validate, preview, and
post with checksum confirmation. Expected invariants are:

- 42 source item rows
- 152 non-zero balances
- Party/site: 20,637
- Godown: 25,382
- Combined: 46,019
- 152 posted opening ledger rows
- No operational/commercial documents created

Re-upload and repost must not duplicate inventory. Reversal must retain the
original rows and create compensating rows.

## Workflow coverage

The tested workflow is quotation → send → approve → PDF → agreement → document
→ activate → order → partial/full issue → receiving → damage/loss → repair/scrap
→ site transfer → billing → invoice → payment/TDS/deposit → outstanding →
reports/GST exports.

CSRF clients must first GET `/api/v1/auth/csrf`, retain both session and
`XSRF-TOKEN` cookies, and send the cookie value in `X-XSRF-TOKEN` on mutations.

## Scale generation

Run `e2e,local-scale` only against an explicitly named local scale database and
set:

```text
stocksync.scale.generator.enabled=true
stocksync.scale.generator.exit=true
stocksync.scale.profile=LARGE
stocksync.scale.seed=20260728
```

LARGE targets 1,200 items, 180 parties, 540 sites, 1,200 agreements, 5,000
orders, 10,000 invoices, and 100,000 stock transactions. The seed makes the
dataset repeatable.

## Verification

Inventory reconciliation derives balances from opening stock and business
documents, then compares those values with projection tables, dashboard, and
reports. Financial reconciliation uses:

```text
outstanding = issued invoices - posted cash - posted TDS - posted deposit adjustments
```

Cancelled invoices and reversed settlements are excluded.

Run backend tests and package with Java 21. Run frontend tests and build. Review
the complete machine-readable evidence in `e2e-data/results`, especially
`inventory-reconciliation.json`, `financial-reconciliation.json`,
`role-permission-results.json`, and `failed-scenarios.json`.

## Result

The run verdict is `READY_WITH_MINOR_ISSUES`. See
`e2e-data/results/e2e-summary.md` for exact counts, timings, resolved defects,
environment deviations, and remaining issues.

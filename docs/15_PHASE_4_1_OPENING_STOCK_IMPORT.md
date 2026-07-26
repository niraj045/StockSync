# Phase 4.1 — Client Opening Stock Import

## Purpose

Phase 4.1 migrates the client's legacy opening-stock snapshot into StockSync without pretending
the spreadsheet is the normalized domain model. The workbook is staging evidence. Item, Party,
Site, and the immutable Inventory ledger remain authoritative after confirmed posting.

No Phase 5 challan behavior is included.

## Client Artifacts

- Original: `client-data/original/Stock of material as on 25-07-26.xlsx`
- Prepared: `client-data/prepared/StockSync_Client_Opening_Stock_Import.xlsx`

The prepared workbook preserves the original `Sheet1` and adds:

- `StockSync Item Mapping` with exact names, `MAT-001` through `MAT-042`, decision fields, and
  highlighted ambiguous rows;
- `StockSync Location Mapping` with all 16 exact source labels and party/site decision fields;
- `Import Read Me` with source rules, corrected totals, dates, and opening-balance meaning.

Production parsing uses Apache POI and only the controlled `STEELFAB_STOCK_SNAPSHOT_V1` layout.

## Authoritative Source Structure

| Source | Meaning | Handling |
|---|---|---|
| `Sheet1` column B | Exact source item name | Preserved and mapped |
| Columns C:R | Sixteen party/site balances | Parsed individually |
| Column T | Source party total | Ignored; formula omits P:R |
| Column U | Godown balance | Parsed individually |
| Column V | Source combined total | Ignored |

The parser finds 42 source material rows, 120 non-zero party/site cells, 32 non-zero godown
cells, and 152 normalized balance rows. Blank and zero cells are omitted. Negative or nonnumeric
quantities are rejected.

## Corrected Totals and Dates

| Scope | Source result | Correct result | Snapshot date |
|---|---:|---:|---|
| Party/site issued | 18,219 | 20,637 | 25-07-2026 |
| Godown available | 25,382 | 25,382 | 16-07-2026 |
| Combined owned | 43,601 | 46,019 | Mixed dates |

Columns P:R—ANV, Rocks & Logs, and Innovator Façade—contribute 2,418 units omitted by the source
party formula. The combined 46,019 is useful for ownership reconciliation but is not a same-date
physical count.

## Mapping Workflow

Item resolution priority is:

1. active explicit alias for this source format;
2. existing item selected by code;
3. exact case-insensitive item-name match;
4. manually selected item;
5. newly created item after confirmation.

New imported items default to category `SCAFFOLDING` and unit `PIECE`. Identity fields remain
required. Unknown weight, financial fields, and minimum-stock threshold stay null. Normal item
creation still requires a minimum-stock value; only confirmed legacy import creation can preserve
that unknown threshold.

The two `8ft & 10ft  plate pipe` rows (Sr.No. 33 and 35) are never automatically merged.
`Damage H frame` requires a separate-item or documented-exclusion decision because StockSync
does not yet have a damaged opening bucket. `Out size H frame` requires a separate-item/variant
or exclusion decision.

Each source location label is preserved and mapped once for its entire column. The user must
select or create a legal Party, then select or create an open Site beneath it. The source label is
not automatically assumed to be both.

## Posting Meaning and Transaction

After validation, the dry run separates party/site and godown expected, mapped, excluded,
unresolved, and posted totals. Posting is blocked until each scope reconciles and every
nonexcluded row is resolved.

ADMIN posting requires explicit confirmation plus the batch's SHA-256 checksum. One database
transaction:

1. pessimistically locks the batch;
2. rechecks status, checksum, mappings, and totals;
3. processes affected item balances in deterministic item-ID order;
4. appends one immutable transaction for every nonexcluded staging row;
5. updates the balance projection;
6. marks rows and batch posted;
7. records the audit action.

Godown cells create `OPENING_GODOWN_BALANCE` transactions into `AVAILABLE`. Party/site cells
create `OPENING_SITE_BALANCE` transactions into `ISSUED`. Both increase total owned stock.

These are opening-state records. They do not create a purchase, vendor transaction, order,
issued challan, fake issue date, or historical rental charge.

## Idempotency and Reversal

Workbook checksum, unique batch code, locked batch state, and row posting references prevent
repeat posting. A posted batch and its original transactions are never deleted or edited.

Only ADMIN may reverse. A nonblank reason is mandatory. Inventory locks all affected balances
and blocks reversal when any later stock movement exists for those items. A safe reversal appends
one compensating transaction per original row, updates the projections atomically, retains the
original transaction link, and records `STOCK_IMPORT_REVERSED`. A batch cannot reverse twice.

## Roles

| Capability | ADMIN | OPERATIONS | ACCOUNTS | VIEWER |
|---|---:|---:|---:|---:|
| View batches, rows, preview, report | Yes | Yes | Yes | Yes |
| Upload, map, validate | Yes | Yes | No | No |
| Post | Yes | No | No | No |
| Reverse | Yes | No | No | No |

## Audit Actions

- `STOCK_IMPORT_UPLOADED`
- `STOCK_IMPORT_PARSED`
- `STOCK_IMPORT_MAPPING_UPDATED`
- `STOCK_IMPORT_VALIDATED`
- `STOCK_IMPORT_POSTED`
- `STOCK_IMPORT_FAILED`
- `STOCK_IMPORT_REVERSED`

File contents are not written to audit logs.

## Known Client Questions

1. Confirm whether each of the 16 source columns represents a legal party, a site, or a project.
2. Confirm whether `Damage H frame` should become a separate item or remain excluded until a
   damaged-stock bucket is introduced.
3. Confirm whether `Out size H frame` is a separate item or variant.
4. Confirm corrections for shorthand and possible misspellings before creating items.
5. Confirm whether Sr.No. 33 and 35 are truly separate material definitions despite identical
   source names.

## Manual Acceptance Flow

1. Open `/opening-stock-imports` and upload the original workbook.
2. Verify 42 source rows, 152 balance rows, and the mixed-date warning.
3. Verify corrected totals: 20,637 party/site, 25,382 godown, 46,019 combined.
4. Map all 16 location columns.
5. Resolve or document exclusions for every ambiguous item, including Sr.No. 2, 3, 33, and 35.
6. Validate and inspect the dry-run party/godown reconciliation.
7. As ADMIN, confirm the checksum and post.
8. Verify 120 `OPENING_SITE_BALANCE` and 32 `OPENING_GODOWN_BALANCE` ledger rows.
9. Verify the stock projection and audit report; verify no purchase, order, or challan was
   created.
10. Restart the application and confirm batch/report history remains.
11. Use a separate isolated test batch to verify reversal and later-movement protection.

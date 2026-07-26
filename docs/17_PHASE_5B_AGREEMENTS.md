# Phase 5B — Agreements

Phase 5B implements quotation-derived agreement management only. Site Orders, challans, stock reservation, rental billing, payments and GST workflows remain outside this phase.

## Conversion and numbering

`POST /api/v1/agreements/from-quotation/{quotationId}` converts an approved quotation in one transaction. The quotation is locked, repeated conversion returns the existing linked agreement, and the quotation becomes `CONVERTED`. Agreement numbers use an independent atomic financial-year sequence, for example `AGR/2026-27/0001`, and are never recycled.

## Historical snapshots

V10 stores party, site, source-quotation and item snapshots. PDF output and API responses use those snapshots, so later master-data edits do not rewrite historical commercial records.

## Workflow

`DRAFT → READY_FOR_REVIEW → ACTIVE → EXPIRED/CLOSED`, with explicit return-to-draft, termination and cancellation paths. Only drafts are commercially editable. A generated PDF is required before activation, and only one active agreement per site is allowed.

- DRAFT: update, mark ready, generate preview PDF, cancel.
- READY_FOR_REVIEW: return to draft, regenerate PDF, activate, cancel.
- ACTIVE: immutable; expire, terminate with a reason, or close.
- EXPIRED: close.
- TERMINATED, CLOSED and CANCELLED: final.

## Rental configuration

The agreement preserves rental type, weekly/monthly/custom billing cycle, custom cycle days, grace period, minimum billing days, security deposit, contracted quantities, rates, loss rates and damage rates. Phase 5B does not calculate invoices.

## Documents and permissions

PDFs are generated from stored snapshots, saved through authenticated `file_attachments`, and downloaded from `GET /api/v1/agreements/{id}/document` with safe filenames. An ACTIVE document cannot be replaced.

- ADMIN: full workflow.
- OPERATIONS: convert, edit drafts, prepare review, generate and download.
- ROLE_ACCOUNTS and VIEWER: read and download only.

## Manual verification

1. Create, send and approve a quotation.
2. Convert it from Agreements; repeat conversion and confirm the same agreement is returned.
3. Verify snapshots, edit dates/rental terms, and mark ready.
4. Confirm activation fails before PDF generation.
5. Generate/download PDF, activate, and confirm ACTIVE is read-only.
6. Confirm a second active agreement for the same site is blocked.
7. Change party/site/item masters and verify historical agreement output is unchanged.
8. Test termination/cancellation reasons on separate agreements.
9. Confirm stock balances and stock transactions do not change.

## Known limitations

Expiry is not scheduler-driven; future operations must validate status and date. Template administration was not expanded. Site Orders stay disabled pending Phase 5C.

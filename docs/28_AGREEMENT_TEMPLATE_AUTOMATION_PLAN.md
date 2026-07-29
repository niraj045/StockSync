# Agreement Template Automation Plan

## Goal

Allow an administrator to upload agreement PDFs received from different parties, preserve the
original legal document, and create reusable layouts that generate future agreements with the
same design without a paid AI service.

The first reference layout is:

`22. Rocks & Logs (Bandra site Tower).pdf`

## Product Approach

StockSync will use a hybrid workflow instead of promising automatic conversion of every PDF:

1. Preserve every uploaded source PDF unchanged.
2. Extract searchable text with PDFBox; use local Tesseract OCR only when a PDF has no usable
   text layer.
3. Let an administrator map dynamic fields visually for an unknown layout.
4. Save the mapping as a versioned party-specific template.
5. Reuse the mapping automatically for later agreements with the same layout.
6. Generate variable-length commercial schedules with StockSync HTML/PDF rendering and append
   them when they cannot safely fit inside the source layout.
7. Require human review before an imported or generated agreement can become active.

This keeps client data private and has no per-document API cost.

## Supported Template Modes

### Native dynamic layout

An HTML/CSS layout owned by StockSync. It supports repeating item tables, conditional sections,
automatic pagination, totals, terms and signature blocks. This is the preferred mode for layouts
that StockSync must reproduce closely and frequently.

The Rocks & Logs layout will be the first native dynamic layout.

### PDF field overlay

The original PDF is used as a fixed background and values are placed at administrator-defined
page coordinates. This is suitable for dates, names, identifiers, deposits and other fixed-size
fields.

### Original PDF plus generated schedule

The client PDF remains unchanged and StockSync appends a standardized commercial schedule for
items, quantities, rates and totals. This is the safe fallback for long or variable content.

## Template Lifecycle

Templates are versioned and immutable after use:

`DRAFT -> VALIDATED -> ACTIVE -> RETIRED`

An agreement stores the exact template version used to generate it. Editing an active template
creates a new version and never changes historical documents.

## Agreement Document Lifecycle

StockSync stores separate attachments for:

- original party PDF;
- generated preview;
- generated final PDF;
- client-signed copy;
- countersigned copy.

Generated previews may be replaced while the agreement is a draft. Activated agreement documents
are immutable; later changes require an amendment or a new template version.

## Dynamic Field Contract

Initial scalar fields:

- agreement number and dates;
- source quotation number and date;
- party legal/trade name, GSTIN, PAN, address and contact;
- site name, code, address and contact;
- billing cycle, billing start/end rules and minimum billing days;
- security deposit;
- subtotal, charges, tax and grand total;
- terms and notes.

Initial repeating item fields:

- sequence;
- item code, name, description, size and unit;
- contracted quantity;
- rental type and rental rate;
- area and weight rates;
- loss and damage rates;
- slab rates and notes.

## Administration Experience

The template workspace will provide:

1. Upload source PDF.
2. Detect text or request local OCR.
3. Choose native layout, overlay, or appended schedule.
4. Map and validate required fields.
5. Preview with sample agreement data.
6. Compare the preview beside the source PDF.
7. Activate a version.

Unknown or conflicting values are always marked for review. For example, the Rocks & Logs sample
mentions both a six-month commitment and a 90-day minimum period; StockSync must not choose one
automatically.

## Delivery Phases

### Phase 1 — safe foundation

- Add template type, status, code and version lineage.
- Add source/reference PDF attachment support.
- Record the exact template version and generation metadata on an agreement document.
- Keep the current built-in agreement renderer as the default.

### Phase 2 — Rocks & Logs native template

- Recreate the five-page A4 layout using printable HTML/CSS.
- Bind agreement and item fields to stored snapshots.
- Support repeating item rows, pagination, totals and amount in words.
- Add golden PDF text/content tests and a visual comparison checklist.

### Phase 3 — reusable PDF overlay mapper

- Render PDF pages in the browser.
- Add drag-and-drop scalar field placement.
- Store normalized page coordinates and formatting rules.
- Render overlays with PDFBox.

### Phase 4 — local extraction

- Extract text and positions with PDFBox.
- Add optional OCRmyPDF/Tesseract processing for scanned documents.
- Suggest fields using deterministic labels, regular expressions and table headers.
- Save administrator corrections as reusable party/layout profiles.

### Phase 5 — generated schedule and package

- Generate a StockSync commercial schedule for variable-length data.
- Merge the original PDF, schedule and signature/approval pages.
- Preserve checksums and an auditable attachment chain.

## Acceptance Criteria for the First Layout

- The supplied Rocks & Logs PDF remains unchanged as the reference.
- An administrator can select the Rocks & Logs layout for an agreement.
- Party, site, dates, items, rates, deposit, taxes and terms come from agreement snapshots.
- More item rows paginate without overlapping terms or signatures.
- Generated totals agree with persisted commercial totals.
- A preview can be downloaded before activation.
- The generated file is stored through authenticated attachment storage.
- Activated agreements cannot have their generated document silently replaced.
- No external AI API or per-document service is required.

# 9. Reporting and Document Processing

## 9.1 Document Technologies

- Apache POI for DOCX and XLSX
- Thymeleaf for HTML templates
- OpenHTMLtoPDF for PDF generation
- Apache PDFBox for PDF extraction, merge, and post-processing

## 9.2 Agreement Processing

### Initial Supported Workflow

1. User uploads a supported agreement template.
2. System validates file type.
3. System extracts known fields or placeholders.
4. System creates an editable draft.
5. User reviews items, quantities, rates, and totals.
6. System generates final document.
7. Final document is stored against the site.

### Recommended Template Format

Use known placeholders:

```text
{{partyName}}
{{siteName}}
{{agreementDate}}
{{gstin}}
{{securityDeposit}}
{{totalAmount}}
{{items}}
```

Avoid claiming support for arbitrary scanned agreements in version one.

## 9.3 PDF Templates

Create templates for:

- Quotation
- Agreement summary
- Issued challan
- Receiving challan
- Invoice
- Proforma invoice
- Monthly site report
- Site ledger
- GST summary

## 9.4 Monthly Site Report Content

- Party details
- Site details
- Reporting month
- Opening pending quantity
- Issued quantities
- Received quantities
- Lost quantities
- Damaged quantities
- Closing pending quantity
- Item weights
- Rental amount
- Additional charges
- GST
- Payments
- TDS
- Outstanding balance

## 9.5 Export Formats

- PDF
- XLSX
- JSON where required for GST

## 9.6 File Storage

Recommended path:

```text
/app/storage/
├── agreements/
├── quotations/
├── challans/
│   ├── issued/
│   └── received/
├── invoices/
├── party-documents/
├── site-documents/
├── reports/
└── temp/
```

## 9.7 File Security

- Store outside public web root.
- Download through authenticated API.
- Generate unique stored names.
- Keep original names as metadata.
- Prevent path traversal.
- Validate content type.
- Limit file size.
- Optionally calculate checksum.
- Delete temporary files after processing.

## 9.8 Report Generation Strategy

Generate small reports synchronously.

Move to background jobs only if reports become large or slow.

For the initial three-user deployment, synchronous generation is sufficient.

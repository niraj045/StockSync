package com.stocksync.billing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksync.audit.service.UserActivityLogService;
import com.stocksync.auth.entity.User;
import com.stocksync.auth.repository.UserRepository;
import com.stocksync.billing.dto.InvoiceDtos.*;
import com.stocksync.billing.entity.*;
import com.stocksync.billing.repository.*;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.common.numbering.DocumentNumberService;
import com.stocksync.common.numbering.DocumentType;
import com.stocksync.common.pdf.PdfBranding;
import com.stocksync.file.entity.FileAttachment;
import com.stocksync.file.repository.FileAttachmentRepository;
import com.stocksync.quotation.entity.Quotation;
import com.stocksync.quotation.entity.QuotationTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class InvoiceService {

    private final InvoiceRepository invoices;
    private final InvoiceItemRepository invoiceItems;
    private final BillingRunRepository billingRuns;
    private final BillingSourceAllocationRepository allocations;
    private final FileAttachmentRepository attachments;
    private final InvoicePdfService pdf;
    private final DocumentNumberService numbers;
    private final UserRepository users;
    private final UserActivityLogService audit;
    private final ObjectMapper objectMapper;
    private final Path storageRoot;

    public InvoiceService(
            InvoiceRepository invoices,
            InvoiceItemRepository invoiceItems,
            BillingRunRepository billingRuns,
            BillingSourceAllocationRepository allocations,
            FileAttachmentRepository attachments,
            InvoicePdfService pdf,
            DocumentNumberService numbers,
            UserRepository users,
            UserActivityLogService audit,
            ObjectMapper objectMapper,
            @Value("${stocksync.file-storage-path}") String root) {
        this.invoices = invoices;
        this.invoiceItems = invoiceItems;
        this.billingRuns = billingRuns;
        this.allocations = allocations;
        this.attachments = attachments;
        this.pdf = pdf;
        this.numbers = numbers;
        this.users = users;
        this.audit = audit;
        this.objectMapper = objectMapper;
        this.storageRoot = Paths.get(root).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> list(Long agreementId, Long partyId, Long siteId, String status, Pageable pageable) {
        Specification<Invoice> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> p = new ArrayList<>();
            if (agreementId != null) p.add(cb.equal(root.get("agreement").get("id"), agreementId));
            if (partyId != null) p.add(cb.equal(root.get("party").get("id"), partyId));
            if (siteId != null) p.add(cb.equal(root.get("site").get("id"), siteId));
            if (status != null && !status.isBlank()) {
                p.add(cb.equal(root.get("status"), InvoiceStatus.valueOf(status.toUpperCase())));
            }
            return cb.and(p.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return invoices.findAll(spec, pageable).map(this::response);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse get(Long id) {
        return response(require(id));
    }

    @Transactional
    public InvoiceResponse createFromBillingRun(Long billingRunId, HttpServletRequest http) {
        BillingRun br = billingRuns.findById(billingRunId)
                .orElseThrow(() -> new BusinessRuleException("BILLING_RUN_NOT_FOUND", "Billing run not found"));

        if (br.getStatus() != BillingRunStatus.FINALIZED) {
            throw new BusinessRuleException("BILLING_RUN_NOT_FINALIZED", "Invoice can only be generated from a finalized billing run");
        }

        Optional<Invoice> existing = invoices.findByBillingRunId(billingRunId);
        if (existing.isPresent()) {
            return response(existing.get());
        }

        Invoice i = new Invoice();
        i.setInvoiceNumber(numbers.next(DocumentType.INVOICE, LocalDate.now()));
        i.setBillingRun(br);
        i.setAgreement(br.getAgreement());
        i.setParty(br.getParty());
        i.setSite(br.getSite());
        i.setInvoiceDate(LocalDate.now());
        i.setDueDate(LocalDate.now().plusDays(paymentDueDays(br.getAgreement().getQuotation())));
        i.setPeriodStart(br.getPeriodStart());
        i.setPeriodEnd(br.getPeriodEnd());
        i.setStatus(InvoiceStatus.DRAFT);

        // Fetch company snapshots from template
        String companyName = PdfBranding.COMPANY_NAME;
        String companyAddress = "";
        String companyGstin = "";

        Quotation q = br.getAgreement().getQuotation();
        if (q != null && q.getQuotationTemplate() != null) {
            QuotationTemplate t = q.getQuotationTemplate();
            companyName = valueOr(t.getCompanyName(), companyName);
            companyAddress = valueOr(t.getCompanyAddress(), companyAddress);
            companyGstin = valueOr(t.getCompanyGstin(), companyGstin);
        }

        i.setCompanyNameSnapshot(companyName);
        i.setCompanyAddressSnapshot(companyAddress);
        i.setCompanyGstinSnapshot(companyGstin);

        // Customer Snapshots
        i.setPartyLegalNameSnapshot(br.getAgreement().getPartyLegalNameSnapshot());
        i.setPartyGstinSnapshot(br.getAgreement().getPartyGstinSnapshot());
        i.setPartyPanSnapshot(br.getAgreement().getPartyPanSnapshot());
        i.setPartyAddressSnapshot(br.getAgreement().getPartyAddressSnapshot());
        i.setPartyStateSnapshot(br.getAgreement().getPartyStateSnapshot());

        i.setSiteNameSnapshot(br.getAgreement().getSiteNameSnapshot());
        i.setSiteCodeSnapshot(br.getAgreement().getSiteCodeSnapshot());
        i.setSiteAddressSnapshot(br.getAgreement().getSiteAddressSnapshot());
        i.setSiteContactSnapshot(br.getAgreement().getSiteContactSnapshot());
        i.setAgreementNumberSnapshot(br.getAgreement().getAgreementNumber());

        // Totals
        i.setSubtotal(br.getRentalSubtotal());
        i.setDiscountAmount(br.getDiscountAmount());
        i.setTaxableAmount(br.getTaxableAmount());
        i.setCgstRate(br.getCgstRate());
        i.setCgstAmount(br.getCgstAmount());
        i.setSgstRate(br.getSgstRate());
        i.setSgstAmount(br.getSgstAmount());
        i.setIgstRate(br.getIgstRate());
        i.setIgstAmount(br.getIgstAmount());
        i.setTotalTax(br.getTotalTax());
        i.setRoundOff(br.getRoundOff());
        i.setGrandTotal(br.getGrandTotal());
        i.setOutstandingAmount(br.getGrandTotal());

        i.setTerms(br.getAgreement().getTerms());
        i.setCreatedBy(actor());
        i.setUpdatedBy(actor());

        List<InvoiceItem> items = new ArrayList<>();
        int seq = 1;

        // Rental Segment Items
        for (BillingRunSegment s : br.getSegments()) {
            InvoiceItem item = new InvoiceItem();
            item.setLineType("RENTAL");
            item.setAgreementItem(s.getAgreementItem());
            item.setItem(s.getItem());
            item.setItemCodeSnapshot(s.getItemCodeSnapshot());
            item.setItemNameSnapshot(s.getItemNameSnapshot());
            item.setSizeSnapshot(s.getSizeSnapshot());
            item.setUnitSnapshot(s.getUnitSnapshot());
            item.setDescription(s.getCalculationExplanation());
            item.setQuantity(s.getQuantity());
            item.setArea(s.getArea());
            item.setWeight(s.getWeightSnapshot());
            item.setBillableDays(s.getBillableDays());
            item.setRate(s.getBaseRate());
            item.setTaxable(true);
            item.setAmount(s.getAmount());
            item.setSequenceNumber(seq++);
            i.addItem(item);
        }

        // Operational Charge Items
        for (BillingRunCharge c : br.getCharges()) {
            if (c.isSelected()) {
                InvoiceItem item = new InvoiceItem();
                item.setLineType(c.getChargeType());
                item.setSourceType(c.getSourceType());
                item.setSourceId(c.getSourceId());
                item.setSourceDocumentNumber(c.getSourceDocumentNumber());
                item.setDescription(c.getDescription());
                item.setQuantity(c.getQuantity());
                item.setRate(c.getRate());
                item.setTaxable(c.isTaxable());
                item.setAmount(c.getAmount());
                item.setSequenceNumber(seq++);
                i.addItem(item);
            }
        }

        // Manual Adjustment
        if (br.getManualAdjustmentTotal().signum() != 0) {
            InvoiceItem item = new InvoiceItem();
            item.setLineType("MANUAL_ADJUSTMENT");
            item.setDescription("Manual authorized operational adjustments");
            item.setQuantity(BigDecimal.ONE);
            item.setRate(br.getManualAdjustmentTotal());
            item.setTaxable(true);
            item.setAmount(br.getManualAdjustmentTotal());
            item.setSequenceNumber(seq++);
            i.addItem(item);
        }

        // Discount Line
        if (br.getDiscountAmount().signum() > 0) {
            InvoiceItem item = new InvoiceItem();
            item.setLineType("DISCOUNT");
            item.setDescription(String.format("Discount applied (%s %s)", br.getDiscountType(), br.getDiscountValue().stripTrailingZeros().toPlainString()));
            item.setQuantity(BigDecimal.ONE);
            item.setRate(br.getDiscountAmount().negate());
            item.setTaxable(true);
            item.setAmount(br.getDiscountAmount().negate());
            item.setSequenceNumber(seq++);
            i.addItem(item);
        }

        Invoice saved = invoices.save(i);
        audit("INVOICE_CREATED", "Invoice", saved.getId(), "Generated draft invoice " + saved.getInvoiceNumber() + " from run " + br.getBillingRunNumber(), http);
        return response(saved);
    }

    @Transactional
    public InvoiceResponse update(Long id, InvoiceUpdateRequest r, HttpServletRequest http) {
        Invoice i = require(id);
        if (i.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessRuleException("INVALID_STATUS", "Only draft invoices can be updated");
        }
        if (i.getVersion() != r.version()) {
            throw new ObjectOptimisticLockingFailureException(Invoice.class, id);
        }

        i.setDueDate(r.dueDate());
        if (r.terms() != null) i.setTerms(r.terms().trim());
        if (r.notes() != null) i.setNotes(r.notes().trim());

        i.setUpdatedBy(actor());
        Invoice saved = invoices.save(i);
        audit("INVOICE_UPDATED", "Invoice", saved.getId(), "Updated metadata for invoice " + saved.getInvoiceNumber(), http);
        return response(saved);
    }

    @Transactional
    public InvoiceResponse generatePdf(Long id, HttpServletRequest http) {
        Invoice i = require(id);
        
        byte[] bytes = pdf.generate(response(i));
        String filename = "invoice-" + i.getInvoiceNumber().replaceAll("[^A-Za-z0-9.-]", "-") + ".pdf";
        Path dir = storageRoot.resolve("invoices").resolve(String.valueOf(i.getId())).normalize();
        Path target = dir.resolve(UUID.randomUUID() + ".pdf").normalize();

        if (!target.startsWith(storageRoot)) {
            throw new BusinessRuleException("INVALID_FILE_PATH", "Invalid target file path");
        }

        try {
            Files.createDirectories(dir);
            Files.write(target, bytes);
        } catch (IOException e) {
            try { Files.deleteIfExists(target); } catch (Exception ignored) {}
            throw new BusinessRuleException("PDF_GENERATION_FAILED", "Unable to write invoice PDF file: " + e.getMessage());
        }

        FileAttachment f = new FileAttachment();
        f.setEntityType("INVOICE");
        f.setEntityId(i.getId());
        f.setDocumentType("INVOICE_PDF");
        f.setOriginalFilename(filename);
        f.setStoredFilename(target.getFileName().toString());
        f.setContentType("application/pdf");
        f.setFileSize(bytes.length);
        f.setStoragePath(storageRoot.relativize(target).toString());
        f.setDescription("System generated tax invoice PDF");
        f.setUploadedBy(actor());
        f = attachments.save(f);

        i.setGeneratedPdf(f);
        i.setUpdatedBy(actor());
        Invoice saved = invoices.save(i);
        audit("INVOICE_PDF_GENERATED", "Invoice", saved.getId(), "Generated PDF file for invoice " + saved.getInvoiceNumber(), http);
        return response(saved);
    }

    @Transactional
    public InvoiceResponse issue(Long id, HttpServletRequest http) {
        Invoice i = require(id);
        if (i.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessRuleException("INVALID_STATUS", "Only draft invoices can be issued");
        }

        if (i.getGeneratedPdf() == null) {
            generatePdf(i.getId(), http);
            i = require(id); // Reload
        }

        i.setStatus(InvoiceStatus.ISSUED);
        i.setIssuedAt(Instant.now());
        i.setIssuedBy(actor());
        i.setUpdatedBy(actor());

        Invoice saved = invoices.save(i);
        audit("INVOICE_ISSUED", "Invoice", saved.getId(), "Issued tax invoice " + saved.getInvoiceNumber(), http);
        return response(saved);
    }

    @Transactional
    public InvoiceResponse cancel(Long id, String reason, HttpServletRequest http) {
        Invoice i = require(id);
        if (i.getStatus() == InvoiceStatus.CANCELLED) {
            return response(i);
        }

        // Release allocations from the billing run so they can be billed again
        allocations.deleteByBillingRunId(i.getBillingRun().getId());

        i.setStatus(InvoiceStatus.CANCELLED);
        i.setCancelledAt(Instant.now());
        i.setCancelledBy(actor());
        i.setCancellationReason(reason);
        i.setUpdatedBy(actor());

        Invoice saved = invoices.save(i);
        audit("INVOICE_CANCELLED", "Invoice", saved.getId(), "Cancelled invoice " + saved.getInvoiceNumber() + ", reason: " + reason, http);
        return response(saved);
    }

    @Transactional(readOnly = true)
    public Download downloadPdf(Long id) {
        Invoice i = require(id);
        FileAttachment f = i.getGeneratedPdf();
        if (f == null) {
            throw new BusinessRuleException("PDF_NOT_FOUND", "Invoice document has not been generated yet");
        }

        Path path = storageRoot.resolve(f.getStoragePath()).normalize();
        if (!path.startsWith(storageRoot) || !Files.isRegularFile(path)) {
            throw new BusinessRuleException("FILE_NOT_FOUND", "Invoice PDF file not found on disk");
        }

        return new Download(new FileSystemResource(path), f.getOriginalFilename(), f.getContentType());
    }

    private Invoice require(Long id) {
        return invoices.findDetailedById(id)
                .orElseThrow(() -> new BusinessRuleException("INVOICE_NOT_FOUND", "Invoice not found"));
    }

    private InvoiceResponse response(Invoice i) {
        List<InvoiceItemResponse> itemResponses = i.getItems().stream()
                .map(item -> new InvoiceItemResponse(
                        item.getId(),
                        item.getLineType(),
                        item.getAgreementItem() == null ? null : item.getAgreementItem().getId(),
                        item.getSourceType(),
                        item.getSourceId(),
                        item.getSourceDocumentNumber(),
                        item.getItem() == null ? null : item.getItem().getId(),
                        item.getItemCodeSnapshot(),
                        item.getItemNameSnapshot(),
                        item.getSizeSnapshot(),
                        item.getUnitSnapshot(),
                        item.getDescription(),
                        item.getQuantity(),
                        item.getArea(),
                        item.getWeight(),
                        item.getBillableDays(),
                        item.getRate(),
                        item.isTaxable(),
                        item.getAmount(),
                        item.getSequenceNumber()
                )).toList();

        return new InvoiceResponse(
                i.getId(),
                i.getInvoiceNumber(),
                i.getBillingRun().getId(),
                i.getBillingRun().getBillingRunNumber(),
                i.getAgreement().getId(),
                i.getAgreementNumberSnapshot(),
                i.getCompanyNameSnapshot(),
                i.getCompanyAddressSnapshot(),
                i.getCompanyGstinSnapshot(),
                i.getPartyLegalNameSnapshot(),
                i.getPartyGstinSnapshot(),
                i.getPartyAddressSnapshot(),
                i.getPartyStateSnapshot(),
                i.getSiteNameSnapshot(),
                i.getSiteCodeSnapshot(),
                i.getSiteAddressSnapshot(),
                i.getSiteContactSnapshot(),
                i.getInvoiceDate(),
                i.getDueDate(),
                i.getPeriodStart(),
                i.getPeriodEnd(),
                i.getStatus().name(),
                i.getSubtotal(),
                i.getDiscountAmount(),
                i.getTaxableAmount(),
                i.getCgstRate(),
                i.getCgstAmount(),
                i.getSgstRate(),
                i.getSgstAmount(),
                i.getIgstRate(),
                i.getIgstAmount(),
                i.getTotalTax(),
                i.getRoundOff(),
                i.getGrandTotal(),
                i.getCashAllocatedTotal(),
                i.getTdsAllocatedTotal(),
                i.getDepositAdjustedTotal(),
                i.getOutstandingAmount(),
                paymentStatus(i.getGrandTotal(), i.getOutstandingAmount()),
                i.getTerms(),
                i.getNotes(),
                i.getGeneratedPdf() == null ? null : i.getGeneratedPdf().getId(),
                i.getIssuedAt(),
                i.getIssuedBy(),
                i.getCancelledAt(),
                i.getCancelledBy(),
                i.getCancellationReason(),
                i.getVersion(),
                itemResponses
        );
    }

    private User currentUser() {
        return users.findByUsernameIgnoreCase(actor()).orElse(null);
    }

    private String paymentStatus(BigDecimal total, BigDecimal outstanding) {
        if (outstanding == null || outstanding.signum() <= 0) return "PAID";
        if (total != null && outstanding.compareTo(total) < 0) return "PARTIALLY_PAID";
        return "UNPAID";
    }

    private int paymentDueDays(Quotation quotation) {
        if (quotation == null || quotation.getExactHireFieldsJson() == null
                || quotation.getExactHireFieldsJson().isBlank()) return 30;
        try {
            JsonNode value = objectMapper.readTree(quotation.getExactHireFieldsJson()).get("paymentDueDays");
            return value == null || !value.canConvertToInt() ? 30 : Math.max(0, value.asInt());
        } catch (IOException ignored) {
            return 30;
        }
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private void audit(String action, String entity, long id, String desc, HttpServletRequest request) {
        User u = currentUser();
        audit.log(u == null ? null : u.getId(), actor(), action, entity, String.valueOf(id), desc, request);
    }

    private String actor() {
        var a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? "system" : a.getName();
    }

    public record Download(Resource resource, String filename, String contentType) {}
}

package com.stocksync.payment.service;

import com.stocksync.audit.service.UserActivityLogService;
import com.stocksync.auth.entity.User;
import com.stocksync.auth.repository.UserRepository;
import com.stocksync.billing.entity.Invoice;
import com.stocksync.billing.entity.InvoiceStatus;
import com.stocksync.billing.repository.InvoiceRepository;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.common.numbering.DocumentNumberService;
import com.stocksync.common.numbering.DocumentType;
import com.stocksync.file.entity.FileAttachment;
import com.stocksync.file.repository.FileAttachmentRepository;
import com.stocksync.party.entity.Party;
import com.stocksync.party.repository.PartyRepository;
import com.stocksync.payment.dto.PaymentDtos.*;
import com.stocksync.payment.entity.*;
import com.stocksync.payment.repository.*;
import com.stocksync.site.entity.Site;
import com.stocksync.site.repository.SiteRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
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

@Service
public class PaymentService {
    private static final Set<PaymentMode> REFERENCE_REQUIRED = EnumSet.of(PaymentMode.BANK_TRANSFER, PaymentMode.UPI, PaymentMode.NEFT, PaymentMode.RTGS, PaymentMode.IMPS);
    private final PaymentReceiptRepository payments;
    private final TdsDetailsRepository tdsDetails;
    private final InvoiceRepository invoices;
    private final PartyRepository parties;
    private final SiteRepository sites;
    private final FileAttachmentRepository attachments;
    private final DocumentNumberService numbers;
    private final PaymentReceiptPdfService pdf;
    private final UserRepository users;
    private final UserActivityLogService audit;
    private final Path storageRoot;

    public PaymentService(PaymentReceiptRepository payments, TdsDetailsRepository tdsDetails, InvoiceRepository invoices,
            PartyRepository parties, SiteRepository sites, FileAttachmentRepository attachments, DocumentNumberService numbers,
            PaymentReceiptPdfService pdf, UserRepository users, UserActivityLogService audit,
            @Value("${stocksync.file-storage-path}") String root) {
        this.payments = payments; this.tdsDetails = tdsDetails; this.invoices = invoices; this.parties = parties; this.sites = sites;
        this.attachments = attachments; this.numbers = numbers; this.pdf = pdf; this.users = users; this.audit = audit;
        this.storageRoot = Paths.get(root).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> list(String search, Long partyId, Long siteId, String status, String paymentMode, LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        Specification<PaymentReceipt> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> p = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String q = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("receiptNumber")), q), cb.like(cb.lower(root.get("partyNameSnapshot")), q)));
            }
            if (partyId != null) p.add(cb.equal(root.get("party").get("id"), partyId));
            if (siteId != null) p.add(cb.equal(root.get("site").get("id"), siteId));
            if (status != null && !status.isBlank()) p.add(cb.equal(root.get("status"), PaymentStatus.valueOf(status.toUpperCase(Locale.ROOT))));
            if (paymentMode != null && !paymentMode.isBlank()) p.add(cb.equal(root.get("paymentMode"), PaymentMode.valueOf(paymentMode.toUpperCase(Locale.ROOT))));
            if (dateFrom != null) p.add(cb.greaterThanOrEqualTo(root.get("paymentDate"), dateFrom));
            if (dateTo != null) p.add(cb.lessThanOrEqualTo(root.get("paymentDate"), dateTo));
            return cb.and(p.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return payments.findAll(spec, pageable).map(this::response);
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(Long id) { return response(requireDetailed(id)); }

    @Transactional
    public PaymentResponse create(PaymentRequest r, HttpServletRequest http) {
        Party party = parties.findById(r.partyId()).orElseThrow(() -> new BusinessRuleException("PARTY_NOT_FOUND", "Party not found"));
        Site site = r.siteId() == null ? null : sites.findById(r.siteId()).orElseThrow(() -> new BusinessRuleException("SITE_NOT_FOUND", "Site not found"));
        if (site != null && !site.getParty().getId().equals(party.getId())) throw new BusinessRuleException("PAYMENT_PARTY_MISMATCH", "Site does not belong to payment party");
        PaymentReceipt p = new PaymentReceipt();
        p.setReceiptNumber(numbers.next(DocumentType.PAYMENT_RECEIPT, r.paymentDate()));
        applyDraft(p, r, party, site);
        p.setCreatedBy(actor()); p.setUpdatedBy(actor());
        PaymentReceipt saved = payments.save(p);
        audit("PAYMENT_CREATED", "PaymentReceipt", saved.getId(), "Created payment draft " + saved.getReceiptNumber(), http);
        return response(saved);
    }

    @Transactional
    public PaymentResponse update(Long id, PaymentRequest r, HttpServletRequest http) {
        PaymentReceipt p = payments.findForUpdate(id).orElseThrow(() -> new BusinessRuleException("PAYMENT_NOT_FOUND", "Payment not found"));
        if (p.getStatus() != PaymentStatus.DRAFT) throw new BusinessRuleException("INVALID_PAYMENT_STATUS", "Only draft payments can be edited");
        if (r.version() == null || p.getVersion() != r.version()) throw new ObjectOptimisticLockingFailureException(PaymentReceipt.class, id);
        Party party = parties.findById(r.partyId()).orElseThrow(() -> new BusinessRuleException("PARTY_NOT_FOUND", "Party not found"));
        Site site = r.siteId() == null ? null : sites.findById(r.siteId()).orElseThrow(() -> new BusinessRuleException("SITE_NOT_FOUND", "Site not found"));
        if (site != null && !site.getParty().getId().equals(party.getId())) throw new BusinessRuleException("PAYMENT_PARTY_MISMATCH", "Site does not belong to payment party");
        applyDraft(p, r, party, site);
        p.setUpdatedBy(actor());
        PaymentReceipt saved = payments.save(p);
        audit("PAYMENT_UPDATED", "PaymentReceipt", saved.getId(), "Updated payment draft " + saved.getReceiptNumber(), http);
        return response(saved);
    }

    @Transactional
    public PaymentResponse post(Long id, HttpServletRequest http) {
        PaymentReceipt p = payments.findForUpdate(id).orElseThrow(() -> new BusinessRuleException("PAYMENT_NOT_FOUND", "Payment not found"));
        if (p.getStatus() == PaymentStatus.POSTED) return response(p);
        if (p.getStatus() != PaymentStatus.DRAFT) throw new BusinessRuleException("INVALID_PAYMENT_STATUS", "Only draft payments can be posted");
        applyAllocations(p, List.copyOf(p.getAllocations()), true);
        p.setStatus(PaymentStatus.POSTED);
        p.setPostedAt(Instant.now());
        p.setPostedBy(actor());
        p.setUpdatedBy(actor());
        PaymentReceipt saved = payments.save(p);
        audit("PAYMENT_POSTED", "PaymentReceipt", saved.getId(), "Posted payment " + saved.getReceiptNumber(), http);
        return response(saved);
    }

    @Transactional
    public PaymentResponse allocate(Long id, AllocateRequest r, HttpServletRequest http) {
        PaymentReceipt p = payments.findForUpdate(id).orElseThrow(() -> new BusinessRuleException("PAYMENT_NOT_FOUND", "Payment not found"));
        if (p.getStatus() != PaymentStatus.POSTED) throw new BusinessRuleException("INVALID_PAYMENT_STATUS", "Only posted payments can allocate remaining advance");
        if (p.getUnallocatedAmount().signum() <= 0) throw new BusinessRuleException("INSUFFICIENT_ADVANCE_BALANCE", "No advance is available on this payment");
        applyAllocations(p, toEntities(p, r.allocations()), false);
        p.setUpdatedBy(actor());
        PaymentReceipt saved = payments.save(p);
        audit("PAYMENT_ALLOCATED", "PaymentReceipt", saved.getId(), "Allocated advance from payment " + saved.getReceiptNumber(), http);
        return response(saved);
    }

    @Transactional
    public PaymentResponse reverse(Long id, String reason, HttpServletRequest http) {
        PaymentReceipt p = payments.findForUpdate(id).orElseThrow(() -> new BusinessRuleException("PAYMENT_NOT_FOUND", "Payment not found"));
        if (p.getStatus() == PaymentStatus.REVERSED) return response(p);
        if (p.getStatus() != PaymentStatus.POSTED) throw new BusinessRuleException("INVALID_PAYMENT_STATUS", "Only posted payments can be reversed");
        if (reason == null || reason.isBlank()) throw new BusinessRuleException("PAYMENT_REVERSAL_REASON_REQUIRED", "Reversal reason is required");
        for (PaymentAllocation a : p.getAllocations()) {
            Invoice i = invoices.findForUpdate(a.getInvoice().getId()).orElseThrow(() -> new BusinessRuleException("INVOICE_NOT_FOUND", "Invoice not found"));
            i.setCashAllocatedTotal(minZero(i.getCashAllocatedTotal().subtract(a.getCashAllocated())));
            i.setTdsAllocatedTotal(minZero(i.getTdsAllocatedTotal().subtract(a.getTdsAllocated())));
            recomputeOutstanding(i);
            i.setUpdatedBy(actor());
        }
        p.setStatus(PaymentStatus.REVERSED);
        p.setUnallocatedAmount(BigDecimal.ZERO);
        p.setReversedAt(Instant.now());
        p.setReversedBy(actor());
        p.setReversalReason(reason.trim());
        p.setUpdatedBy(actor());
        PaymentReceipt saved = payments.save(p);
        audit("PAYMENT_REVERSED", "PaymentReceipt", saved.getId(), "Reversed payment " + saved.getReceiptNumber() + ", reason: " + reason, http);
        return response(saved);
    }

    @Transactional
    public TdsDetailsResponse updateTds(Long id, TdsDetailsRequest r, HttpServletRequest http) {
        PaymentReceipt p = requireDetailed(id);
        if (p.getTdsAmount().signum() <= 0) throw new BusinessRuleException("INVALID_PAYMENT_STATUS", "Payment has no TDS amount");
        TdsDetails details = Optional.ofNullable(p.getTdsDetails()).orElseGet(TdsDetails::new);
        details.setPaymentReceipt(p);
        details.setTdsAmount(p.getTdsAmount());
        details.setDeductionDate(r.deductionDate());
        details.setSection(blank(r.section()));
        details.setCertificateNumber(blank(r.certificateNumber()));
        details.setCertificateDate(r.certificateDate());
        if (r.certificateAttachmentId() != null) {
            details.setCertificateAttachment(attachments.findById(r.certificateAttachmentId()).orElseThrow(() -> new BusinessRuleException("FILE_NOT_FOUND", "Certificate attachment not found")));
        }
        details.setVerificationStatus(TdsVerificationStatus.PENDING);
        details.setRejectionReason(null);
        details.setUpdatedBy(actor());
        TdsDetails saved = tdsDetails.save(details);
        audit("TDS_DETAILS_UPDATED", "PaymentReceipt", p.getId(), "Updated TDS certificate details for " + p.getReceiptNumber(), http);
        return tdsResponse(saved);
    }

    @Transactional
    public TdsDetailsResponse verifyTds(Long id, HttpServletRequest http) {
        TdsDetails d = tdsDetails.findByPaymentReceiptId(id).orElseThrow(() -> new BusinessRuleException("TDS_DETAILS_NOT_FOUND", "TDS details not found"));
        d.setVerificationStatus(TdsVerificationStatus.VERIFIED);
        d.setVerifiedAt(Instant.now());
        d.setVerifiedBy(actor());
        d.setRejectionReason(null);
        d.setUpdatedBy(actor());
        audit("TDS_VERIFIED", "PaymentReceipt", id, "Verified TDS for payment " + d.getPaymentReceipt().getReceiptNumber(), http);
        return tdsResponse(d);
    }

    @Transactional
    public TdsDetailsResponse rejectTds(Long id, String reason, HttpServletRequest http) {
        TdsDetails d = tdsDetails.findByPaymentReceiptId(id).orElseThrow(() -> new BusinessRuleException("TDS_DETAILS_NOT_FOUND", "TDS details not found"));
        d.setVerificationStatus(TdsVerificationStatus.REJECTED);
        d.setRejectionReason(reason.trim());
        d.setUpdatedBy(actor());
        audit("TDS_REJECTED", "PaymentReceipt", id, "Rejected TDS for payment " + d.getPaymentReceipt().getReceiptNumber() + ", reason: " + reason, http);
        return tdsResponse(d);
    }

    @Transactional(readOnly = true)
    public List<EligibleInvoiceResponse> eligibleInvoices(Long partyId) {
        return invoices.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("party").get("id"), partyId),
                cb.equal(root.get("status"), InvoiceStatus.ISSUED),
                cb.greaterThan(root.get("outstandingAmount"), BigDecimal.ZERO)
        )).stream().map(this::eligible).toList();
    }

    @Transactional(readOnly = true)
    public AdvanceResponse availableAdvance(Long partyId) {
        BigDecimal total = payments.findAll((root, query, cb) -> cb.and(cb.equal(root.get("party").get("id"), partyId), cb.equal(root.get("status"), PaymentStatus.POSTED)))
                .stream().map(PaymentReceipt::getUnallocatedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AdvanceResponse(partyId, total);
    }

    @Transactional
    public Download generateReceipt(Long id, HttpServletRequest http) {
        PaymentReceipt p = requireDetailed(id);
        if (p.getStatus() != PaymentStatus.POSTED && p.getStatus() != PaymentStatus.REVERSED) throw new BusinessRuleException("INVALID_PAYMENT_STATUS", "Only posted receipts can be downloaded");
        if (p.getAttachment() == null) {
            byte[] bytes = pdf.generate(response(p));
            String filename = "payment-receipt-" + p.getReceiptNumber().replaceAll("[^A-Za-z0-9.-]", "-") + ".pdf";
            Path dir = storageRoot.resolve("payment-receipts").resolve(String.valueOf(p.getId())).normalize();
            Path target = dir.resolve(UUID.randomUUID() + ".pdf").normalize();
            if (!target.startsWith(storageRoot)) throw new BusinessRuleException("INVALID_FILE_PATH", "Invalid target file path");
            try {
                Files.createDirectories(dir);
                Files.write(target, bytes);
            } catch (IOException e) {
                throw new BusinessRuleException("PDF_GENERATION_FAILED", "Unable to write receipt PDF file");
            }
            FileAttachment f = new FileAttachment();
            f.setEntityType("PAYMENT_RECEIPT"); f.setEntityId(p.getId()); f.setDocumentType("PAYMENT_RECEIPT_PDF");
            f.setOriginalFilename(filename); f.setStoredFilename(target.getFileName().toString()); f.setContentType("application/pdf"); f.setFileSize(bytes.length);
            f.setStoragePath(storageRoot.relativize(target).toString()); f.setDescription("System generated payment receipt PDF"); f.setUploadedBy(actor());
            p.setAttachment(attachments.save(f)); p.setUpdatedBy(actor()); payments.save(p);
            audit("PAYMENT_RECEIPT_GENERATED", "PaymentReceipt", p.getId(), "Generated receipt PDF for " + p.getReceiptNumber(), http);
        }
        Path path = storageRoot.resolve(p.getAttachment().getStoragePath()).normalize();
        return new Download(new FileSystemResource(path), p.getAttachment().getOriginalFilename(), p.getAttachment().getContentType());
    }

    private void applyDraft(PaymentReceipt p, PaymentRequest r, Party party, Site site) {
        BigDecimal cash = money(r.cashAmount()), tds = money(r.tdsAmount());
        if (cash.signum() < 0 || tds.signum() < 0) throw new BusinessRuleException("INVALID_PAYMENT_AMOUNT", "Cash and TDS cannot be negative");
        if (cash.add(tds).signum() <= 0) throw new BusinessRuleException("INVALID_PAYMENT_AMOUNT", "At least cash or TDS must be greater than zero");
        PaymentMode mode = PaymentMode.valueOf(r.paymentMode().toUpperCase(Locale.ROOT));
        if (REFERENCE_REQUIRED.contains(mode) && isBlank(r.referenceNumber())) throw new BusinessRuleException("PAYMENT_REFERENCE_REQUIRED", "Electronic payment modes require a reference number");
        if (mode == PaymentMode.CHEQUE && (isBlank(r.chequeNumber()) || r.chequeDate() == null)) throw new BusinessRuleException("PAYMENT_CHEQUE_DETAILS_REQUIRED", "Cheque number and date are required");
        p.setParty(party); p.setSite(site); p.setPartyNameSnapshot(party.getLegalName()); p.setSiteNameSnapshot(site == null ? null : site.getSiteName());
        p.setPaymentDate(r.paymentDate()); p.setPaymentMode(mode); p.setReferenceNumber(blank(r.referenceNumber())); p.setBankName(blank(r.bankName()));
        p.setChequeNumber(blank(r.chequeNumber())); p.setChequeDate(r.chequeDate()); p.setCashAmount(cash); p.setTdsAmount(tds);
        p.setTotalSettlementAmount(cash.add(tds)); p.setUnallocatedAmount(cash.add(tds)); p.setNotes(blank(r.notes()));
        p.getAllocations().clear();
        for (PaymentAllocation a : toEntities(p, Optional.ofNullable(r.allocations()).orElse(List.of()))) p.addAllocation(a);
        if (tds.signum() > 0 && p.getTdsDetails() == null) {
            TdsDetails details = new TdsDetails(); details.setTdsAmount(tds); details.setCreatedBy(actor()); details.setUpdatedBy(actor()); p.setTdsDetails(details);
        }
    }

    private List<PaymentAllocation> toEntities(PaymentReceipt p, List<AllocationRequest> requests) {
        return requests.stream().map(r -> {
            PaymentAllocation a = new PaymentAllocation();
            a.setPaymentReceipt(p);
            a.setInvoice(invoices.getReferenceById(r.invoiceId()));
            a.setCashAllocated(money(r.cashAllocated()));
            a.setTdsAllocated(money(r.tdsAllocated()));
            a.setTotalAllocated(a.getCashAllocated().add(a.getTdsAllocated()));
            a.setCreatedBy(actor()); a.setUpdatedBy(actor());
            return a;
        }).toList();
    }

    private void applyAllocations(PaymentReceipt p, List<PaymentAllocation> allocations, boolean initialPost) {
        BigDecimal availableCash = initialPost ? p.getCashAmount() : p.getUnallocatedAmount();
        BigDecimal availableTds = initialPost ? p.getTdsAmount() : BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO, cash = BigDecimal.ZERO, tds = BigDecimal.ZERO;
        Set<Long> seenInvoices = new HashSet<>();
        for (PaymentAllocation draft : allocations) {
            Invoice i = invoices.findForUpdate(draft.getInvoice().getId()).orElseThrow(() -> new BusinessRuleException("INVOICE_NOT_FOUND", "Invoice not found"));
            if (!seenInvoices.add(i.getId())) throw new BusinessRuleException("PAYMENT_ALLOCATION_DUPLICATE_INVOICE", "Duplicate invoice allocation is not allowed");
            if (i.getStatus() != InvoiceStatus.ISSUED) throw new BusinessRuleException("INVOICE_NOT_ELIGIBLE_FOR_PAYMENT", "Only issued invoices can receive allocations");
            if (!i.getParty().getId().equals(p.getParty().getId())) throw new BusinessRuleException("PAYMENT_PARTY_MISMATCH", "Payment party must match invoice party");
            if (p.getSite() != null && !i.getSite().getId().equals(p.getSite().getId())) throw new BusinessRuleException("PAYMENT_PARTY_MISMATCH", "Payment site must match invoice site");
            if (draft.getCashAllocated().signum() < 0 || draft.getTdsAllocated().signum() < 0) throw new BusinessRuleException("PAYMENT_ALLOCATION_EXCEEDS_AVAILABLE_AMOUNT", "Allocations cannot be negative");
            BigDecimal line = draft.getTotalAllocated();
            if (line.signum() <= 0) throw new BusinessRuleException("PAYMENT_ALLOCATION_EXCEEDS_AVAILABLE_AMOUNT", "Allocation amount must be positive");
            if (line.compareTo(i.getOutstandingAmount()) > 0) throw new BusinessRuleException("INVOICE_ALLOCATION_EXCEEDS_OUTSTANDING", "Allocation exceeds invoice outstanding");
            cash = cash.add(draft.getCashAllocated()); tds = tds.add(draft.getTdsAllocated()); total = total.add(line);
            i.setCashAllocatedTotal(i.getCashAllocatedTotal().add(draft.getCashAllocated()));
            i.setTdsAllocatedTotal(i.getTdsAllocatedTotal().add(draft.getTdsAllocated()));
            recomputeOutstanding(i);
            i.setUpdatedBy(actor());
        }
        if (cash.compareTo(availableCash) > 0 || tds.compareTo(availableTds) > 0 || total.compareTo(availableCash.add(availableTds)) > 0) {
            throw new BusinessRuleException("PAYMENT_ALLOCATION_EXCEEDS_AVAILABLE_AMOUNT", "Allocation exceeds available payment amount");
        }
        if (!initialPost) {
            for (PaymentAllocation a : allocations) p.addAllocation(a);
        }
        p.setUnallocatedAmount(availableCash.add(availableTds).subtract(total));
    }

    private void recomputeOutstanding(Invoice i) {
        BigDecimal outstanding = i.getGrandTotal().subtract(i.getCashAllocatedTotal()).subtract(i.getTdsAllocatedTotal()).subtract(i.getDepositAdjustedTotal());
        i.setOutstandingAmount(outstanding.signum() < 0 ? BigDecimal.ZERO : outstanding);
    }

    private PaymentReceipt requireDetailed(Long id) {
        return payments.findDetailedById(id).orElseThrow(() -> new BusinessRuleException("PAYMENT_NOT_FOUND", "Payment not found"));
    }

    private PaymentResponse response(PaymentReceipt p) {
        return new PaymentResponse(p.getId(), p.getReceiptNumber(), p.getParty().getId(), p.getPartyNameSnapshot(),
                p.getSite() == null ? null : p.getSite().getId(), p.getSiteNameSnapshot(), p.getPaymentDate(), p.getPaymentMode().name(),
                p.getReferenceNumber(), p.getBankName(), p.getChequeNumber(), p.getChequeDate(), p.getCashAmount(), p.getTdsAmount(),
                p.getTotalSettlementAmount(), p.getUnallocatedAmount(), p.getStatus().name(), p.getNotes(),
                p.getAttachment() == null ? null : p.getAttachment().getId(), p.getPostedAt(), p.getPostedBy(), p.getReversedAt(), p.getReversedBy(), p.getReversalReason(),
                p.getCreatedAt(), p.getCreatedBy(), p.getUpdatedAt(), p.getUpdatedBy(), p.getVersion(),
                p.getAllocations().stream().map(a -> new PaymentAllocationResponse(a.getId(), a.getInvoice().getId(), a.getInvoice().getInvoiceNumber(), a.getCashAllocated(), a.getTdsAllocated(), a.getTotalAllocated())).toList(),
                p.getTdsDetails() == null ? null : tdsResponse(p.getTdsDetails()));
    }

    private TdsDetailsResponse tdsResponse(TdsDetails d) {
        return new TdsDetailsResponse(d.getId(), d.getTdsAmount(), d.getDeductionDate(), d.getSection(), d.getCertificateNumber(), d.getCertificateDate(),
                d.getCertificateAttachment() == null ? null : d.getCertificateAttachment().getId(), d.getVerificationStatus().name(), d.getRejectionReason(), d.getVerifiedAt(), d.getVerifiedBy(), d.getVersion());
    }

    private EligibleInvoiceResponse eligible(Invoice i) {
        return new EligibleInvoiceResponse(i.getId(), i.getInvoiceNumber(), i.getAgreement().getId(), i.getAgreementNumberSnapshot(), i.getSite().getId(), i.getSiteNameSnapshot(), i.getInvoiceDate(), i.getDueDate(), i.getGrandTotal(), i.getOutstandingAmount(), paymentStatus(i));
    }

    private String paymentStatus(Invoice i) {
        if (i.getOutstandingAmount().signum() <= 0) return "PAID";
        if (i.getOutstandingAmount().compareTo(i.getGrandTotal()) < 0) return "PARTIALLY_PAID";
        return "UNPAID";
    }

    private BigDecimal money(BigDecimal v) { return Optional.ofNullable(v).orElse(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal minZero(BigDecimal v) { return v.signum() < 0 ? BigDecimal.ZERO : v; }
    private boolean isBlank(String v) { return v == null || v.isBlank(); }
    private String blank(String v) { return isBlank(v) ? null : v.trim(); }
    private String actor() { var a = SecurityContextHolder.getContext().getAuthentication(); return a == null ? "system" : a.getName(); }
    private User currentUser() { return users.findByUsernameIgnoreCase(actor()).orElse(null); }
    private void audit(String action, String entity, long id, String desc, HttpServletRequest request) {
        User u = currentUser(); audit.log(u == null ? null : u.getId(), actor(), action, entity, String.valueOf(id), desc, request);
    }
    public record Download(Resource resource, String filename, String contentType) {}
}

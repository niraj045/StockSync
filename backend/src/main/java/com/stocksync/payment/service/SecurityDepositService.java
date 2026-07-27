package com.stocksync.payment.service;

import com.stocksync.agreement.entity.Agreement;
import com.stocksync.agreement.repository.AgreementRepository;
import com.stocksync.audit.service.UserActivityLogService;
import com.stocksync.auth.entity.User;
import com.stocksync.auth.repository.UserRepository;
import com.stocksync.billing.entity.Invoice;
import com.stocksync.billing.entity.InvoiceStatus;
import com.stocksync.billing.repository.InvoiceRepository;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.common.numbering.DocumentNumberService;
import com.stocksync.common.numbering.DocumentType;
import com.stocksync.payment.dto.SecurityDepositDtos.*;
import com.stocksync.payment.entity.*;
import com.stocksync.payment.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecurityDepositService {
    private final SecurityDepositTransactionRepository deposits;
    private final DepositInvoiceAllocationRepository depositAllocations;
    private final AgreementRepository agreements;
    private final InvoiceRepository invoices;
    private final DocumentNumberService numbers;
    private final UserRepository users;
    private final UserActivityLogService audit;

    public SecurityDepositService(SecurityDepositTransactionRepository deposits, DepositInvoiceAllocationRepository depositAllocations,
            AgreementRepository agreements, InvoiceRepository invoices, DocumentNumberService numbers, UserRepository users, UserActivityLogService audit) {
        this.deposits = deposits; this.depositAllocations = depositAllocations; this.agreements = agreements; this.invoices = invoices;
        this.numbers = numbers; this.users = users; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Page<DepositTransactionResponse> list(Long agreementId, Long partyId, Long siteId, Long invoiceId, String status, Pageable pageable) {
        Specification<SecurityDepositTransaction> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> p = new ArrayList<>();
            if (agreementId != null) p.add(cb.equal(root.get("agreement").get("id"), agreementId));
            if (partyId != null) p.add(cb.equal(root.get("party").get("id"), partyId));
            if (siteId != null) p.add(cb.equal(root.get("site").get("id"), siteId));
            if (invoiceId != null) p.add(cb.equal(root.get("relatedInvoice").get("id"), invoiceId));
            if (status != null && !status.isBlank()) p.add(cb.equal(root.get("status"), SecurityDepositStatus.valueOf(status.toUpperCase(Locale.ROOT))));
            return cb.and(p.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return deposits.findAll(spec, pageable).map(this::response);
    }

    @Transactional(readOnly = true)
    public DepositTransactionResponse get(Long id) { return response(require(id)); }

    @Transactional
    public DepositTransactionResponse receipt(DepositReceiptRequest r, HttpServletRequest http) {
        Agreement a = agreement(r.agreementId());
        SecurityDepositTransaction d = base(a, SecurityDepositTransactionType.RECEIPT, r.transactionDate(), r.amount(), r.notes());
        d.setPaymentMode(PaymentMode.valueOf(r.paymentMode().toUpperCase(Locale.ROOT)));
        d.setReferenceNumber(blank(r.referenceNumber()));
        SecurityDepositTransaction saved = deposits.save(d);
        audit("SECURITY_DEPOSIT_RECEIVED", "SecurityDeposit", saved.getId(), "Received security deposit " + saved.getDepositNumber(), http);
        return response(saved);
    }

    @Transactional
    public DepositTransactionResponse refund(DepositRefundRequest r, HttpServletRequest http) {
        Agreement a = agreement(r.agreementId());
        BigDecimal amount = money(r.amount());
        if (amount.compareTo(deposits.availableForAgreement(a.getId())) > 0) throw new BusinessRuleException("INSUFFICIENT_DEPOSIT_BALANCE", "Refund exceeds available security deposit");
        SecurityDepositTransaction d = base(a, SecurityDepositTransactionType.REFUND, r.transactionDate(), amount, r.reason());
        d.setPaymentMode(PaymentMode.valueOf(r.paymentMode().toUpperCase(Locale.ROOT)));
        d.setReferenceNumber(blank(r.referenceNumber()));
        SecurityDepositTransaction saved = deposits.save(d);
        audit("SECURITY_DEPOSIT_REFUNDED", "SecurityDeposit", saved.getId(), "Refunded security deposit " + saved.getDepositNumber() + ", reason: " + r.reason(), http);
        return response(saved);
    }

    @Transactional
    public DepositTransactionResponse adjust(DepositAdjustmentRequest r, HttpServletRequest http) {
        Agreement a = agreement(r.agreementId());
        BigDecimal amount = money(r.amount());
        if (amount.compareTo(deposits.availableForAgreement(a.getId())) > 0) throw new BusinessRuleException("INSUFFICIENT_DEPOSIT_BALANCE", "Deposit adjustment exceeds available balance");
        Invoice i = invoices.findForUpdate(r.invoiceId()).orElseThrow(() -> new BusinessRuleException("INVOICE_NOT_FOUND", "Invoice not found"));
        if (i.getStatus() != InvoiceStatus.ISSUED) throw new BusinessRuleException("INVOICE_NOT_ELIGIBLE_FOR_PAYMENT", "Only issued invoices can receive deposit adjustments");
        if (!i.getAgreement().getId().equals(a.getId())) throw new BusinessRuleException("INVALID_DEPOSIT_TRANSACTION", "Invoice does not belong to this agreement");
        if (amount.compareTo(i.getOutstandingAmount()) > 0) throw new BusinessRuleException("DEPOSIT_ADJUSTMENT_EXCEEDS_OUTSTANDING", "Deposit adjustment exceeds invoice outstanding");
        SecurityDepositTransaction d = base(a, SecurityDepositTransactionType.ADJUSTMENT_TO_INVOICE, r.transactionDate(), amount, r.notes());
        d.setRelatedInvoice(i);
        SecurityDepositTransaction saved = deposits.save(d);
        DepositInvoiceAllocation allocation = new DepositInvoiceAllocation();
        allocation.setDepositTransaction(saved); allocation.setInvoice(i); allocation.setAmount(amount); allocation.setCreatedBy(actor()); allocation.setUpdatedBy(actor());
        depositAllocations.save(allocation);
        i.setDepositAdjustedTotal(i.getDepositAdjustedTotal().add(amount));
        recomputeOutstanding(i);
        i.setUpdatedBy(actor());
        audit("SECURITY_DEPOSIT_ADJUSTED", "SecurityDeposit", saved.getId(), "Adjusted security deposit " + saved.getDepositNumber() + " to invoice " + i.getInvoiceNumber(), http);
        return response(saved);
    }

    @Transactional
    public DepositTransactionResponse reverse(Long id, String reason, HttpServletRequest http) {
        SecurityDepositTransaction d = deposits.findForUpdate(id).orElseThrow(() -> new BusinessRuleException("DEPOSIT_NOT_FOUND", "Security deposit transaction not found"));
        if (d.getStatus() == SecurityDepositStatus.REVERSED) return response(d);
        if (reason == null || reason.isBlank()) throw new BusinessRuleException("INVALID_DEPOSIT_TRANSACTION", "Reversal reason is required");
        if (d.getTransactionType() == SecurityDepositTransactionType.ADJUSTMENT_TO_INVOICE && d.getRelatedInvoice() != null) {
            Invoice i = invoices.findForUpdate(d.getRelatedInvoice().getId()).orElseThrow(() -> new BusinessRuleException("INVOICE_NOT_FOUND", "Invoice not found"));
            i.setDepositAdjustedTotal(minZero(i.getDepositAdjustedTotal().subtract(d.getAmount())));
            recomputeOutstanding(i);
            i.setUpdatedBy(actor());
        } else if (d.getTransactionType() == SecurityDepositTransactionType.RECEIPT) {
            BigDecimal availableExcludingThis = deposits.availableForAgreement(d.getAgreement().getId()).subtract(d.getAmount());
            if (availableExcludingThis.signum() < 0) throw new BusinessRuleException("INSUFFICIENT_DEPOSIT_BALANCE", "Cannot reverse receipt after the deposit has been used");
        }
        d.setStatus(SecurityDepositStatus.REVERSED);
        d.setReversedAt(Instant.now());
        d.setReversedBy(actor());
        d.setReversalReason(reason.trim());
        d.setUpdatedBy(actor());
        SecurityDepositTransaction saved = deposits.save(d);
        audit("SECURITY_DEPOSIT_REVERSED", "SecurityDeposit", saved.getId(), "Reversed security deposit " + saved.getDepositNumber() + ", reason: " + reason, http);
        return response(saved);
    }

    @Transactional(readOnly = true)
    public DepositSummaryResponse summary(Long agreementId) {
        Agreement a = agreement(agreementId);
        Object[] row = deposits.totalsForAgreement(agreementId);
        BigDecimal received = (BigDecimal) row[0];
        BigDecimal adjusted = (BigDecimal) row[1];
        BigDecimal refunded = (BigDecimal) row[2];
        BigDecimal available = received.subtract(adjusted).subtract(refunded);
        BigDecimal shortfallOrExcess = available.subtract(a.getSecurityDeposit());
        return new DepositSummaryResponse(a.getId(), a.getAgreementNumber(), a.getSecurityDeposit(), received, adjusted, refunded, available, shortfallOrExcess);
    }

    private SecurityDepositTransaction base(Agreement a, SecurityDepositTransactionType type, LocalDate date, BigDecimal amount, String notes) {
        SecurityDepositTransaction d = new SecurityDepositTransaction();
        d.setDepositNumber(numbers.next(DocumentType.SECURITY_DEPOSIT, date));
        d.setAgreement(a); d.setParty(a.getParty()); d.setSite(a.getSite());
        d.setAgreementNumberSnapshot(a.getAgreementNumber()); d.setPartyNameSnapshot(a.getPartyLegalNameSnapshot()); d.setSiteNameSnapshot(a.getSiteNameSnapshot());
        d.setTransactionType(type); d.setTransactionDate(date); d.setAmount(money(amount)); d.setStatus(SecurityDepositStatus.POSTED);
        d.setPostedAt(Instant.now()); d.setPostedBy(actor()); d.setNotes(blank(notes)); d.setCreatedBy(actor()); d.setUpdatedBy(actor());
        return d;
    }

    private Agreement agreement(Long id) { return agreements.findById(id).orElseThrow(() -> new BusinessRuleException("AGREEMENT_NOT_FOUND", "Agreement not found")); }
    private SecurityDepositTransaction require(Long id) { return deposits.findDetailedById(id).orElseThrow(() -> new BusinessRuleException("DEPOSIT_NOT_FOUND", "Security deposit transaction not found")); }
    private DepositTransactionResponse response(SecurityDepositTransaction d) {
        return new DepositTransactionResponse(d.getId(), d.getDepositNumber(), d.getAgreement().getId(), d.getAgreementNumberSnapshot(),
                d.getParty().getId(), d.getPartyNameSnapshot(), d.getSite().getId(), d.getSiteNameSnapshot(), d.getTransactionType().name(),
                d.getTransactionDate(), d.getAmount(), d.getPaymentMode() == null ? null : d.getPaymentMode().name(), d.getReferenceNumber(),
                d.getRelatedInvoice() == null ? null : d.getRelatedInvoice().getId(), d.getRelatedInvoice() == null ? null : d.getRelatedInvoice().getInvoiceNumber(),
                d.getSourceDepositTransaction() == null ? null : d.getSourceDepositTransaction().getId(), d.getStatus().name(), d.getNotes(),
                d.getPostedAt(), d.getPostedBy(), d.getReversedAt(), d.getReversedBy(), d.getReversalReason(), d.getVersion());
    }
    private void recomputeOutstanding(Invoice i) {
        BigDecimal outstanding = i.getGrandTotal().subtract(i.getCashAllocatedTotal()).subtract(i.getTdsAllocatedTotal()).subtract(i.getDepositAdjustedTotal());
        i.setOutstandingAmount(outstanding.signum() < 0 ? BigDecimal.ZERO : outstanding);
    }
    private BigDecimal money(BigDecimal v) { return Optional.ofNullable(v).orElse(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal minZero(BigDecimal v) { return v.signum() < 0 ? BigDecimal.ZERO : v; }
    private String blank(String v) { return v == null || v.isBlank() ? null : v.trim(); }
    private String actor() { var a = SecurityContextHolder.getContext().getAuthentication(); return a == null ? "system" : a.getName(); }
    private User currentUser() { return users.findByUsernameIgnoreCase(actor()).orElse(null); }
    private void audit(String action, String entity, long id, String desc, HttpServletRequest request) {
        User u = currentUser(); audit.log(u == null ? null : u.getId(), actor(), action, entity, String.valueOf(id), desc, request);
    }
}

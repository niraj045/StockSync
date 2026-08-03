package com.stocksync.billing.service;

import com.stocksync.agreement.entity.Agreement;
import com.stocksync.agreement.entity.AgreementStatus;
import com.stocksync.agreement.repository.AgreementRepository;
import com.stocksync.audit.service.UserActivityLogService;
import com.stocksync.auth.entity.User;
import com.stocksync.auth.repository.UserRepository;
import com.stocksync.billing.dto.BillingRunDtos.*;
import com.stocksync.billing.entity.*;
import com.stocksync.billing.repository.*;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.common.numbering.DocumentNumberService;
import com.stocksync.common.numbering.DocumentType;
import com.stocksync.quotation.entity.DiscountType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BillingRunService {

    private final BillingRunRepository billingRuns;
    private final BillingRunSegmentRepository segments;
    private final BillingRunChargeRepository charges;
    private final BillingSourceAllocationRepository allocations;
    private final AgreementRepository agreements;
    private final InvoiceRepository invoices;
    private final BillingRunCalculationService calculator;
    private final DocumentNumberService numbers;
    private final UserRepository users;
    private final UserActivityLogService audit;
    private final JdbcTemplate jdbc;

    public BillingRunService(
            BillingRunRepository billingRuns,
            BillingRunSegmentRepository segments,
            BillingRunChargeRepository charges,
            BillingSourceAllocationRepository allocations,
            AgreementRepository agreements,
            InvoiceRepository invoices,
            BillingRunCalculationService calculator,
            DocumentNumberService numbers,
            UserRepository users,
            UserActivityLogService audit,
            JdbcTemplate jdbc) {
        this.billingRuns = billingRuns;
        this.segments = segments;
        this.charges = charges;
        this.allocations = allocations;
        this.agreements = agreements;
        this.invoices = invoices;
        this.calculator = calculator;
        this.numbers = numbers;
        this.users = users;
        this.audit = audit;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public Page<BillingRunResponse> list(Long agreementId, Long partyId, Long siteId, String status, Pageable pageable) {
        Specification<BillingRun> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> p = new ArrayList<>();
            if (agreementId != null) p.add(cb.equal(root.get("agreement").get("id"), agreementId));
            if (partyId != null) p.add(cb.equal(root.get("party").get("id"), partyId));
            if (siteId != null) p.add(cb.equal(root.get("site").get("id"), siteId));
            if (status != null && !status.isBlank()) {
                p.add(cb.equal(root.get("status"), BillingRunStatus.valueOf(status.toUpperCase())));
            }
            return cb.and(p.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return billingRuns.findAll(spec, pageable).map(this::response);
    }

    @Transactional(readOnly = true)
    public BillingRunResponse get(Long id) {
        return response(require(id));
    }

    @Transactional
    public BillingRunResponse create(BillingRunRequest r, HttpServletRequest http) {
        Agreement ag = agreements.findById(r.agreementId())
                .orElseThrow(() -> new BusinessRuleException("AGREEMENT_NOT_FOUND", "Agreement not found"));

        if (ag.getStatus() != AgreementStatus.ACTIVE && ag.getStatus() != AgreementStatus.TERMINATED && ag.getStatus() != AgreementStatus.EXPIRED) {
            throw new BusinessRuleException("INVALID_AGREEMENT_STATUS", "Billing runs can only be created for ACTIVE, EXPIRED, or TERMINATED agreements");
        }

        validateDates(ag, r.periodStart(), r.periodEnd());

        // Check overlapping finalized or active periods
        if (billingRuns.existsOverlappingPeriod(ag.getId(), r.periodStart(), r.periodEnd(), BillingRunStatus.CANCELLED)) {
            throw new BusinessRuleException("OVERLAPPING_BILLING_PERIOD", "An active or finalized billing run already overlaps with the requested period");
        }

        BillingRun b = new BillingRun();
        b.setBillingRunNumber(numbers.next(DocumentType.BILLING_RUN, LocalDate.now()));
        b.setAgreement(ag);
        b.setParty(ag.getParty());
        b.setSite(ag.getSite());
        b.setPeriodStart(r.periodStart());
        b.setPeriodEnd(r.periodEnd());
        b.setStatus(BillingRunStatus.DRAFT);

        // Tax treatment is approved on the source quotation and carried into the agreement.
        // Reuse those exact rates instead of guessing from free-form address text.
        var quotation = ag.getQuotation();
        b.setCgstRate(quotation == null ? BigDecimal.ZERO : quotation.getCgstRate());
        b.setSgstRate(quotation == null ? BigDecimal.ZERO : quotation.getSgstRate());
        b.setIgstRate(quotation == null ? BigDecimal.ZERO : quotation.getIgstRate());

        b.setCreatedBy(actor());
        b.setUpdatedBy(actor());

        BillingRun saved = billingRuns.save(b);
        audit("BILLING_RUN_CREATED", "BillingRun", saved.getId(), "Created draft billing run " + saved.getBillingRunNumber(), http);
        return calculate(saved.getId(), http);
    }

    @Transactional
    public BillingRunResponse update(Long id, BillingRunUpdateValuesRequest r, HttpServletRequest http) {
        BillingRun b = require(id);
        if (b.getStatus() != BillingRunStatus.DRAFT && b.getStatus() != BillingRunStatus.CALCULATED) {
            throw new BusinessRuleException("INVALID_STATUS", "Only draft or calculated billing runs can be modified");
        }

        if (r.manualAdjustmentTotal() != null) {
            b.setManualAdjustmentTotal(r.manualAdjustmentTotal());
        }
        if (r.discountType() != null) {
            b.setDiscountType(DiscountType.valueOf(r.discountType().toUpperCase()));
        }
        if (r.discountValue() != null) {
            b.setDiscountValue(r.discountValue());
        }

        if (r.selectedChargeIds() != null) {
            Set<Long> selectedIds = new HashSet<>(r.selectedChargeIds());
            b.getCharges().forEach(c -> c.setSelected(selectedIds.contains(c.getId())));
        }

        calculator.calculateTotals(b);
        b.setUpdatedBy(actor());
        BillingRun saved = billingRuns.save(b);
        audit("BILLING_RUN_UPDATED", "BillingRun", saved.getId(), "Updated adjustments/discounts/charges for run " + saved.getBillingRunNumber(), http);
        return response(saved);
    }

    @Transactional
    public BillingRunResponse calculate(Long id, HttpServletRequest http) {
        BillingRun b = require(id);
        if (b.getStatus() != BillingRunStatus.DRAFT && b.getStatus() != BillingRunStatus.CALCULATED) {
            throw new BusinessRuleException("INVALID_STATUS", "Only draft or calculated runs can be recalculated");
        }

        b.getSegments().clear();
        List<BillingRunSegment> segs = calculator.calculateSegments(b.getAgreement(), b.getPeriodStart(), b.getPeriodEnd());
        segs.forEach(b::addSegment);

        // Retain selection mapping
        Map<String, Boolean> selectionMap = b.getCharges().stream()
                .collect(Collectors.toMap(c -> c.getSourceType() + ":" + c.getSourceId(), BillingRunCharge::isSelected, (x, y) -> x));

        b.getCharges().clear();
        List<BillingRunCharge> freshCharges = getAvailableCharges(b.getAgreement(), b.getPeriodStart(), b.getPeriodEnd());
        for (BillingRunCharge c : freshCharges) {
            String key = c.getSourceType() + ":" + c.getSourceId();
            if (selectionMap.containsKey(key)) {
                c.setSelected(selectionMap.get(key));
            }
            b.addCharge(c);
        }

        calculator.calculateTotals(b);
        b.setStatus(BillingRunStatus.CALCULATED);
        b.setCalculatedAt(Instant.now());
        b.setCalculatedBy(actor());
        b.setUpdatedBy(actor());

        BillingRun saved = billingRuns.save(b);
        audit("BILLING_RUN_CALCULATED", "BillingRun", saved.getId(), "Calculated rental timeline segments for run " + saved.getBillingRunNumber(), http);
        return response(saved);
    }

    @Transactional
    public BillingRunResponse finalizeRun(Long id, HttpServletRequest http) {
        BillingRun b = require(id);
        if (b.getStatus() != BillingRunStatus.CALCULATED) {
            throw new BusinessRuleException("FINALIZATION_FAILED", "Run must be calculated before it can be finalized");
        }
        if (b.getGrandTotal() == null || b.getGrandTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("EMPTY_BILLING_RUN",
                    "This period has no billable rental or selected charges. Add a valid adjustment or choose a period with deployed material before finalizing.");
        }

        // Allocate and protect selected operational charges/dispatches
        for (BillingRunCharge c : b.getCharges()) {
            if (c.isSelected()) {
                if (allocations.existsBySourceTypeAndSourceId(c.getSourceType(), c.getSourceId())) {
                    throw new BusinessRuleException("DUPLICATE_BILLING", "One or more selected charges have already been billed on another invoice: " + c.getDescription());
                }
                BillingSourceAllocation alloc = new BillingSourceAllocation();
                alloc.setAgreement(b.getAgreement());
                alloc.setSourceType(c.getSourceType());
                alloc.setSourceId(c.getSourceId());
                alloc.setBillingRun(b);
                allocations.save(alloc);
            }
        }

        b.setStatus(BillingRunStatus.FINALIZED);
        b.setFinalizedAt(Instant.now());
        b.setFinalizedBy(actor());
        b.setUpdatedBy(actor());

        BillingRun saved = billingRuns.save(b);
        audit("BILLING_RUN_FINALIZED", "BillingRun", saved.getId(), "Finalized billing run " + saved.getBillingRunNumber(), http);
        return response(saved);
    }

    @Transactional
    public BillingRunResponse cancel(Long id, String reason, HttpServletRequest http) {
        BillingRun b = require(id);
        if (b.getStatus() == BillingRunStatus.CANCELLED) {
            return response(b);
        }

        Optional<Invoice> linkedInv = invoices.findByBillingRunId(b.getId());
        if (linkedInv.isPresent() && linkedInv.get().getStatus() != InvoiceStatus.CANCELLED) {
            throw new BusinessRuleException("LINKED_INVOICE_ACTIVE", "Cannot cancel billing run while a non-cancelled invoice is linked to it");
        }

        if (b.getStatus() == BillingRunStatus.FINALIZED) {
            // Release source allocations
            allocations.deleteByBillingRunId(b.getId());
        }

        b.setStatus(BillingRunStatus.CANCELLED);
        b.setCancelledAt(Instant.now());
        b.setCancelledBy(actor());
        b.setCancellationReason(reason);
        b.setUpdatedBy(actor());

        BillingRun saved = billingRuns.save(b);
        audit("BILLING_RUN_CANCELLED", "BillingRun", saved.getId(), "Cancelled billing run " + saved.getBillingRunNumber(), http);
        return response(saved);
    }

    @Transactional(readOnly = true)
    public List<EligibleAgreementResponse> getEligibleAgreements() {
        return agreements.findByStatus(AgreementStatus.ACTIVE).stream()
                .map(ag -> new EligibleAgreementResponse(
                        ag.getId(),
                        ag.getAgreementNumber(),
                        ag.getPartyLegalNameSnapshot(),
                        ag.getSiteNameSnapshot(),
                        ag.getEffectiveDate(),
                        ag.getExpiryDate()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, LocalDate> getSuggestedPeriod(Long agreementId) {
        Agreement ag = agreements.findById(agreementId).orElseThrow();
        List<BillingRun> runs = billingRuns.findByAgreementIdAndStatus(agreementId, BillingRunStatus.FINALIZED);
        
        LocalDate start = ag.getEffectiveDate();
        if (!runs.isEmpty()) {
            LocalDate maxEnd = runs.stream().map(BillingRun::getPeriodEnd).max(LocalDate::compareTo).get();
            start = maxEnd.plusDays(1);
        }

        LocalDate end = start;
        switch (ag.getBillingCycle()) {
            case WEEKLY -> end = start.plusDays(6);
            case MONTHLY -> end = start.plusMonths(1).minusDays(1);
            case CUSTOM -> {
                int customDays = Optional.ofNullable(ag.getCustomBillingCycleDays()).orElse(30);
                end = start.plusDays(customDays - 1);
            }
        }

        if (ag.getExpiryDate() != null && end.isAfter(ag.getExpiryDate())) {
            end = ag.getExpiryDate();
        }
        if (end.isAfter(LocalDate.now())) {
            end = LocalDate.now();
        }

        return Map.of("periodStart", start, "periodEnd", end);
    }

    private void validateDates(Agreement ag, LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new BusinessRuleException("INVALID_PERIOD", "Period end date cannot precede start date");
        }
        if (start.isBefore(ag.getEffectiveDate())) {
            throw new BusinessRuleException("INVALID_PERIOD", "Period cannot begin before agreement effective date (" + ag.getEffectiveDate() + ")");
        }
        if (ag.getExpiryDate() != null && end.isAfter(ag.getExpiryDate())) {
            throw new BusinessRuleException("INVALID_PERIOD", "Period cannot extend beyond agreement expiry date (" + ag.getExpiryDate() + ")");
        }
        if (end.isAfter(LocalDate.now())) {
            throw new BusinessRuleException("INVALID_PERIOD", "Future billing periods are blocked");
        }
    }

    private List<BillingRunCharge> getAvailableCharges(Agreement ag, LocalDate start, LocalDate end) {
        List<BillingRunCharge> chargesList = new ArrayList<>();

        // 1. Dispatch charges (Issued Challans)
        jdbc.query("""
            SELECT ic.id, ic.challan_number, ic.transport_charge, ic.loading_charge, ic.unloading_charge
            FROM issued_challans ic
            JOIN site_orders so ON ic.site_order_id = so.id
            WHERE so.agreement_id = ? AND ic.dispatch_date BETWEEN ? AND ?
            """, (rs, rowNum) -> {
                long id = rs.getLong(1);
                String num = rs.getString(2);
                BigDecimal transport = rs.getBigDecimal(3);
                BigDecimal loading = rs.getBigDecimal(4);
                BigDecimal unloading = rs.getBigDecimal(5);

                addChargeIfValid(chargesList, transport, "ISSUED_CHALLAN_TRANSPORT", id, num, "TRANSPORT", "Transport Charge for Dispatch " + num);
                addChargeIfValid(chargesList, loading, "ISSUED_CHALLAN_LOADING", id, num, "LOADING", "Loading Charge for Dispatch " + num);
                addChargeIfValid(chargesList, unloading, "ISSUED_CHALLAN_UNLOADING", id, num, "UNLOADING", "Unloading Charge for Dispatch " + num);
                return null;
        }, ag.getId(), start, end);

        // 2. Return charges (Receiving Challans)
        jdbc.query("""
            SELECT rc.id, rc.receiving_challan_number, rc.transport_charge, rc.handling_charge
            FROM receiving_challans rc
            WHERE rc.agreement_id = ? AND rc.status = 'POSTED' AND rc.receive_date BETWEEN ? AND ?
            """, (rs, rowNum) -> {
                long id = rs.getLong(1);
                String num = rs.getString(2);
                BigDecimal transport = rs.getBigDecimal(3);
                BigDecimal handling = rs.getBigDecimal(4);

                addChargeIfValid(chargesList, transport, "RECEIVING_CHALLAN_TRANSPORT", id, num, "TRANSPORT", "Transport Charge for Return " + num);
                addChargeIfValid(chargesList, handling, "RECEIVING_CHALLAN_HANDLING", id, num, "OTHER", "Handling Charge for Return " + num);
                return null;
        }, ag.getId(), start, end);

        // 3. Site Transfer charges
        jdbc.query("""
            SELECT st.id, st.transfer_number, st.transport_charge
            FROM site_transfers st
            WHERE st.destination_agreement_id = ? AND st.status = 'POSTED' AND st.transfer_date BETWEEN ? AND ?
            """, (rs, rowNum) -> {
                long id = rs.getLong(1);
                String num = rs.getString(2);
                BigDecimal transport = rs.getBigDecimal(3);

                addChargeIfValid(chargesList, transport, "SITE_TRANSFER_TRANSPORT", id, num, "TRANSPORT", "Transport Charge for Site Transfer " + num);
                return null;
        }, ag.getId(), start, end);

        // Client-chargeable transport, labour, Mathadi, TPI and site expenses.
        jdbc.query("""
            SELECT o.id,o.operation_number,o.operation_type,o.amount
            FROM site_operations o
            WHERE o.site_id=? AND o.charge_to_client=TRUE AND o.status='COMPLETED'
              AND o.operation_date BETWEEN ? AND ?
            """,(rs,rowNum)->{
                long id=rs.getLong("id");String number=rs.getString("operation_number");
                String operationType=rs.getString("operation_type");BigDecimal amount=rs.getBigDecimal("amount");
                String chargeType="TRANSPORT".equals(operationType)?"TRANSPORT":operationType.contains("LABOUR")||"MATHADI".equals(operationType)?"LOADING":"OTHER";
                addChargeIfValid(chargesList,amount,"SITE_OPERATION",id,number,chargeType,
                        operationType.replace('_',' ')+" - "+number);
                return null;
            },ag.getSite().getId(),start,end);

        // 4. Manual Loss charges
        jdbc.query("""
            SELECT sl.id, sl.loss_number, sl.calculated_recovery_amount, i.item_name
            FROM loss_records sl
            JOIN items i ON sl.item_id = i.id
            WHERE sl.agreement_id = ?
              AND sl.status = 'APPROVED' AND sl.loss_date BETWEEN ? AND ?
            """, (rs, rowNum) -> {
                long id = rs.getLong(1);
                String num = rs.getString(2);
                BigDecimal amt = rs.getBigDecimal(3);
                String itemName = rs.getString(4);

                addChargeIfValid(chargesList, amt, "STOCK_LOSS_RECOVERY", id, num, "LOSS_RECOVERY", "Loss Recovery Charge for " + itemName + " (" + num + ")");
                return null;
        }, ag.getId(), start, end);

        // 5. Manual Damage/Repair charges
        jdbc.query("""
            SELECT sd.id, sd.damage_number, sd.calculated_damage_amount, sd.estimated_repair_cost, sd.actual_repair_cost, sd.repairable, i.item_name
            FROM damage_records sd
            JOIN items i ON sd.item_id = i.id
            WHERE sd.agreement_id = ?
              AND sd.status IN ('RECORDED', 'UNDER_REPAIR', 'REPAIRED', 'SCRAPPED')
              AND sd.damage_date BETWEEN ? AND ?
            """, (rs, rowNum) -> {
                long id = rs.getLong(1);
                String num = rs.getString(2);
                BigDecimal dmgAmt = rs.getBigDecimal(3);
                BigDecimal estRep = rs.getBigDecimal(4);
                BigDecimal actRep = rs.getBigDecimal(5);
                boolean repairable = rs.getBoolean(6);
                String itemName = rs.getString(7);

                if (repairable) {
                    BigDecimal chargeAmt = actRep.signum() > 0 ? actRep : estRep;
                    addChargeIfValid(chargesList, chargeAmt, "STOCK_DAMAGE_REPAIR", id, num, "REPAIR_CHARGE", "Repair Cost for " + itemName + " (" + num + ")");
                } else {
                    addChargeIfValid(chargesList, dmgAmt, "STOCK_DAMAGE_RECOVERY", id, num, "DAMAGE_RECOVERY", "Damage Recovery for " + itemName + " (" + num + ")");
                }
                return null;
        }, ag.getId(), start, end);

        return chargesList;
    }

    private void addChargeIfValid(List<BillingRunCharge> list, BigDecimal amount, String type, long id, String docNum, String chargeType, String desc) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            // Check database allocation
            if (!allocations.existsBySourceTypeAndSourceId(type, id)) {
                BillingRunCharge c = new BillingRunCharge();
                c.setSourceType(type);
                c.setSourceId(id);
                c.setSourceDocumentNumber(docNum);
                c.setChargeType(chargeType);
                c.setDescription(desc);
                c.setRate(amount);
                c.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
                c.setTaxable(true);
                c.setSelected(true);
                list.add(c);
            }
        }
    }

    private BillingRun require(Long id) {
        return billingRuns.findDetailedById(id)
                .orElseThrow(() -> new BusinessRuleException("BILLING_RUN_NOT_FOUND", "Billing run not found"));
    }

    private BillingRunResponse response(BillingRun b) {
        List<BillingRunSegmentResponse> segResponses = b.getSegments().stream()
                .map(s -> new BillingRunSegmentResponse(
                        s.getId(),
                        s.getAgreementItem().getId(),
                        s.getItem().getId(),
                        s.getItemCodeSnapshot(),
                        s.getItemNameSnapshot(),
                        s.getSizeSnapshot(),
                        s.getUnitSnapshot(),
                        s.getWeightSnapshot(),
                        s.getSourceIssueReference(),
                        s.getSourceEndReference(),
                        s.getRentalType().name(),
                        s.getQuantity(),
                        s.getArea(),
                        s.getWeight(),
                        s.getSegmentStart(),
                        s.getSegmentEnd(),
                        s.getBillableDays(),
                        s.getBaseRate(),
                        s.getAppliedSlabSnapshot(),
                        s.getAmount(),
                        s.getCalculationExplanation(),
                        s.getSequenceNumber()
                )).toList();

        List<BillingRunChargeResponse> chgResponses = b.getCharges().stream()
                .map(c -> new BillingRunChargeResponse(
                        c.getId(),
                        c.getSourceType(),
                        c.getSourceId(),
                        c.getSourceDocumentNumber(),
                        c.getChargeType(),
                        c.getDescription(),
                        c.getQuantity(),
                        c.getRate(),
                        c.getAmount(),
                        c.isTaxable(),
                        c.isSelected()
                )).toList();

        return new BillingRunResponse(
                b.getId(),
                b.getBillingRunNumber(),
                b.getAgreement().getId(),
                b.getAgreement().getAgreementNumber(),
                b.getParty().getId(),
                b.getPartyLegalNameSnapshot(),
                b.getSite().getId(),
                b.getSiteNameSnapshot(),
                b.getPeriodStart(),
                b.getPeriodEnd(),
                b.getStatus().name(),
                b.getRentalSubtotal(),
                b.getLossChargeTotal(),
                b.getDamageChargeTotal(),
                b.getOperationalChargeTotal(),
                b.getManualAdjustmentTotal(),
                b.getDiscountType().name(),
                b.getDiscountValue(),
                b.getDiscountAmount(),
                b.getTaxableAmount(),
                b.getCgstRate(),
                b.getCgstAmount(),
                b.getSgstRate(),
                b.getSgstAmount(),
                b.getIgstRate(),
                b.getIgstAmount(),
                b.getTotalTax(),
                b.getRoundOff(),
                b.getGrandTotal(),
                b.getCalculatedAt(),
                b.getCalculatedBy(),
                b.getFinalizedAt(),
                b.getFinalizedBy(),
                b.getCancelledAt(),
                b.getCancelledBy(),
                b.getCancellationReason(),
                b.getVersion(),
                segResponses,
                chgResponses
        );
    }

    private User currentUser() {
        return users.findByUsernameIgnoreCase(actor()).orElse(null);
    }

    private void audit(String action, String entity, long id, String desc, HttpServletRequest request) {
        User u = currentUser();
        audit.log(u == null ? null : u.getId(), actor(), action, entity, String.valueOf(id), desc, request);
    }

    private String actor() {
        var a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? "system" : a.getName();
    }
}

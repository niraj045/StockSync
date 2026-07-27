package com.stocksync.billing.service;

import com.stocksync.agreement.entity.*;
import com.stocksync.billing.entity.*;
import com.stocksync.challan.entity.*;
import com.stocksync.exception.entity.*;
import com.stocksync.inventory.entity.Item;
import com.stocksync.quotation.entity.RentalType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BillingRunCalculationService {

    private final JdbcTemplate jdbc;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public BillingRunCalculationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public static class Movement {
        public LocalDate date;
        public BigDecimal quantity;
        public String type;
        public Long refId;
        public String docNum;

        public Movement(LocalDate date, BigDecimal quantity, String type, Long refId, String docNum) {
            this.date = date;
            this.quantity = quantity;
            this.type = type;
            this.refId = refId;
            this.docNum = docNum;
        }
    }

    public static class IssueLot {
        public LocalDate date;
        public BigDecimal remainingQty;
        public String type;
        public Long refId;
        public String docNum;

        public IssueLot(LocalDate date, BigDecimal qty, String type, Long refId, String docNum) {
            this.date = date;
            this.remainingQty = qty;
            this.type = type;
            this.refId = refId;
            this.docNum = docNum;
        }
    }

    public static class MatchSegment {
        public LocalDate start;
        public LocalDate end; // Nullable if unreturned
        public BigDecimal quantity;
        public String startRef;
        public String endRef;

        public MatchSegment(LocalDate start, LocalDate end, BigDecimal quantity, String startRef, String endRef) {
            this.start = start;
            this.end = end;
            this.quantity = quantity;
            this.startRef = startRef;
            this.endRef = endRef;
        }
    }

    public List<BillingRunSegment> calculateSegments(Agreement agreement, LocalDate periodStart, LocalDate periodEnd) {
        List<BillingRunSegment> results = new ArrayList<>();
        int sequence = 1;

        for (AgreementItem agItem : agreement.getItems()) {
            Item item = agItem.getItem();
            List<Movement> inwards = getInwards(agreement, item.getId());
            List<Movement> outwards = getOutwards(agreement, item.getId());

            List<MatchSegment> matched = matchFIFO(inwards, outwards);

            for (MatchSegment ms : matched) {
                // Apply date rules
                LocalDate sBill = ms.start;
                if (agreement.getBillingStartRule() == BillingStartRule.DAY_AFTER_ISSUE) {
                    sBill = sBill.plusDays(1);
                }

                LocalDate eBill = ms.end;
                if (eBill != null && agreement.getBillingEndRule() == BillingEndRule.RETURN_DATE_EXCLUDED) {
                    eBill = eBill.minusDays(1);
                }

                // Apply grace period
                int grace = agreement.getGracePeriodDays();
                LocalDate sRentStart = sBill.plusDays(grace);

                if (eBill != null && sRentStart.isAfter(eBill)) {
                    // Grace period covers the entire duration
                    continue;
                }

                // Apply minimum billing days
                int minDays = agreement.getMinimumBillingDays();
                if (eBill != null) {
                    long lifeDays = ChronoUnit.DAYS.between(sRentStart, eBill) + 1;
                    if (lifeDays < minDays) {
                        eBill = sRentStart.plusDays(minDays - 1);
                    }
                }

                // Intersect with billing period [periodStart, periodEnd]
                LocalDate segStart = sRentStart.isBefore(periodStart) ? periodStart : sRentStart;
                LocalDate segEnd = (eBill == null || eBill.isAfter(periodEnd)) ? periodEnd : eBill;

                if (segStart.isBefore(segEnd) || segStart.isEqual(segEnd)) {
                    int billableDays = (int) ChronoUnit.DAYS.between(segStart, segEnd) + 1;

                    // Calculate rental based on type
                    BigDecimal rate = agItem.getRentalRate();
                    BigDecimal amount = BigDecimal.ZERO;
                    String explanation = "";
                    String appliedSlab = null;

                    if (agItem.getRentalType() == RentalType.SLAB_BASED) {
                        // Calculate rate slab values day by day
                        List<AgreementItemSlab> slabs = new ArrayList<>(agItem.getSlabs());
                        slabs.sort(Comparator.comparingInt(AgreementItemSlab::getStartDay));

                        BigDecimal segAmount = BigDecimal.ZERO;
                        long kStart = ChronoUnit.DAYS.between(sRentStart, segStart) + 1;
                        long kEnd = ChronoUnit.DAYS.between(sRentStart, segEnd) + 1;

                        Map<BigDecimal, Long> slabDaysMap = new LinkedHashMap<>();

                        for (long k = kStart; k <= kEnd; k++) {
                            final long dayOffset = k;
                            AgreementItemSlab matchedSlab = slabs.stream()
                                    .filter(s -> s.getStartDay() <= dayOffset && (s.getEndDay() == null || s.getEndDay() >= dayOffset))
                                    .findFirst()
                                    .orElse(null);

                            BigDecimal dayRate = (matchedSlab != null) ? matchedSlab.getRate() : rate;
                            segAmount = segAmount.add(ms.quantity.multiply(dayRate));

                            slabDaysMap.put(dayRate, slabDaysMap.getOrDefault(dayRate, 0L) + 1);
                        }

                        amount = segAmount.setScale(2, RoundingMode.HALF_UP);
                        rate = slabs.isEmpty() ? rate : slabs.get(0).getRate();

                        List<String> slabExps = new ArrayList<>();
                        for (Map.Entry<BigDecimal, Long> entry : slabDaysMap.entrySet()) {
                            slabExps.add(String.format("%d days @ %s/day", entry.getValue(), entry.getKey().setScale(4, RoundingMode.HALF_UP)));
                        }
                        explanation = String.format("Slab rental for %s %s: %s",
                                ms.quantity.setScale(4, RoundingMode.HALF_UP),
                                item.getUnit(),
                                String.join(", ", slabExps));

                        appliedSlab = slabs.stream()
                                .map(s -> String.format("%d-%s:%s", s.getStartDay(), (s.getEndDay() == null ? "" : s.getEndDay()), s.getRate().toString()))
                                .collect(Collectors.joining("; "));
                    } else {
                        // Standard calculation
                        BigDecimal qtyFactor = ms.quantity;
                        BigDecimal dailyRate = rate;

                        if (agItem.getRentalType() == RentalType.PLATE_AREA_PER_DAY) {
                            BigDecimal area = Optional.ofNullable(agItem.getArea()).orElse(BigDecimal.ONE);
                            dailyRate = agItem.getAreaRate();
                            amount = qtyFactor.multiply(area).multiply(dailyRate).multiply(BigDecimal.valueOf(billableDays));
                            explanation = String.format("Plate area rental for %s %s (Area: %s sq m/pc) @ %s/sq m/day for %d days",
                                    qtyFactor.setScale(4, RoundingMode.HALF_UP),
                                    item.getUnit(),
                                    area.setScale(4, RoundingMode.HALF_UP),
                                    dailyRate.setScale(4, RoundingMode.HALF_UP),
                                    billableDays);
                        } else if (agItem.getRentalType() == RentalType.SCAFFOLD_AREA_PER_DAY) {
                            BigDecimal area = Optional.ofNullable(agItem.getArea()).orElse(BigDecimal.ONE);
                            dailyRate = agItem.getAreaRate();
                            amount = qtyFactor.multiply(area).multiply(dailyRate).multiply(BigDecimal.valueOf(billableDays));
                            explanation = String.format("Scaffold area rental for %s %s (Area: %s sq m/pc) @ %s/sq m/day for %d days",
                                    qtyFactor.setScale(4, RoundingMode.HALF_UP),
                                    item.getUnit(),
                                    area.setScale(4, RoundingMode.HALF_UP),
                                    dailyRate.setScale(4, RoundingMode.HALF_UP),
                                    billableDays);
                        } else if (agItem.getRentalType() == RentalType.PLOT_AREA_PER_DAY) {
                            dailyRate = agItem.getAreaRate();
                            amount = qtyFactor.multiply(dailyRate).multiply(BigDecimal.valueOf(billableDays));
                            explanation = String.format("Plot area rental for %s sq m @ %s/sq m/day for %d days",
                                    qtyFactor.setScale(4, RoundingMode.HALF_UP),
                                    dailyRate.setScale(4, RoundingMode.HALF_UP),
                                    billableDays);
                        } else if (agItem.getRentalType() == RentalType.FIXED_RATE) {
                            amount = dailyRate.multiply(BigDecimal.valueOf(billableDays));
                            explanation = String.format("Fixed rental rate of %s/day for %d days",
                                    dailyRate.setScale(4, RoundingMode.HALF_UP),
                                    billableDays);
                        } else { // PER_PIECE_PER_DAY
                            amount = qtyFactor.multiply(dailyRate).multiply(BigDecimal.valueOf(billableDays));
                            explanation = String.format("Rental for %s %s @ %s/day for %d days",
                                    qtyFactor.setScale(4, RoundingMode.HALF_UP),
                                    item.getUnit(),
                                    dailyRate.setScale(4, RoundingMode.HALF_UP),
                                    billableDays);
                        }
                        amount = amount.setScale(2, RoundingMode.HALF_UP);
                    }

                    BillingRunSegment seg = new BillingRunSegment();
                    seg.setAgreementItem(agItem);
                    seg.setItem(item);
                    seg.setItemCodeSnapshot(agItem.getItemCodeSnapshot());
                    seg.setItemNameSnapshot(agItem.getItemNameSnapshot());
                    seg.setSizeSnapshot(agItem.getSizeSnapshot());
                    seg.setUnitSnapshot(agItem.getUnitSnapshot());
                    seg.setWeightSnapshot(agItem.getWeightSnapshot());
                    seg.setSourceIssueReference(ms.startRef);
                    seg.setSourceEndReference(ms.endRef);
                    seg.setRentalType(agItem.getRentalType());
                    seg.setQuantity(ms.quantity);
                    seg.setArea(agItem.getArea());
                    seg.setWeight(agItem.getWeightSnapshot());
                    seg.setSegmentStart(segStart);
                    seg.setSegmentEnd(segEnd);
                    seg.setBillableDays(billableDays);
                    seg.setBaseRate(rate);
                    seg.setAppliedSlabSnapshot(appliedSlab);
                    seg.setAmount(amount);
                    seg.setCalculationExplanation(explanation);
                    seg.setSequenceNumber(sequence++);
                    results.add(seg);
                }
            }
        }

        return results;
    }

    private List<MatchSegment> matchFIFO(List<Movement> inwards, List<Movement> outwards) {
        List<MatchSegment> segments = new ArrayList<>();
        List<IssueLot> lots = inwards.stream()
                .map(m -> new IssueLot(m.date, m.quantity, m.type, m.refId, m.docNum))
                .collect(Collectors.toList());

        List<Movement> sortedOutwards = outwards.stream()
                .sorted(Comparator.comparing(m -> m.date))
                .toList();

        for (Movement out : sortedOutwards) {
            BigDecimal outQty = out.quantity;
            LocalDate outDate = out.date;
            String endRef = out.type + ":" + out.refId + " (" + out.docNum + ")";

            for (IssueLot lot : lots) {
                if (lot.remainingQty.compareTo(BigDecimal.ZERO) <= 0) continue;

                String startRef = lot.type + ":" + lot.refId + " (" + lot.docNum + ")";

                if (lot.remainingQty.compareTo(outQty) <= 0) {
                    segments.add(new MatchSegment(lot.date, outDate, lot.remainingQty, startRef, endRef));
                    outQty = outQty.subtract(lot.remainingQty);
                    lot.remainingQty = BigDecimal.ZERO;
                } else {
                    segments.add(new MatchSegment(lot.date, outDate, outQty, startRef, endRef));
                    lot.remainingQty = lot.remainingQty.subtract(outQty);
                    outQty = BigDecimal.ZERO;
                    break;
                }

                if (outQty.compareTo(BigDecimal.ZERO) == 0) break;
            }
        }

        // Remaining unreturned lots
        for (IssueLot lot : lots) {
            if (lot.remainingQty.compareTo(BigDecimal.ZERO) > 0) {
                String startRef = lot.type + ":" + lot.refId + " (" + lot.docNum + ")";
                segments.add(new MatchSegment(lot.date, null, lot.remainingQty, startRef, null));
            }
        }

        return segments;
    }

    private List<Movement> getInwards(Agreement agreement, Long itemId) {
        List<Movement> m = new ArrayList<>();

        // Dispatches
        jdbc.query("""
            SELECT ic.dispatch_date, ici.quantity, ic.id, ic.challan_number 
            FROM issued_challans ic
            JOIN issued_challan_items ici ON ici.issued_challan_id = ic.id
            JOIN site_orders so ON ic.site_order_id = so.id
            WHERE so.agreement_id = ? AND ici.item_id = ?
            """, (rs, rowNum) -> new Movement(
                rs.getDate(1).toLocalDate(),
                rs.getBigDecimal(2),
                "ISSUED_CHALLAN",
                rs.getLong(3),
                rs.getString(4)
        ), agreement.getId(), itemId).forEach(m::add);

        // Inward Site Transfers
        jdbc.query("""
            SELECT st.transfer_date, sti.quantity, st.id, st.transfer_number 
            FROM site_transfers st
            JOIN site_transfer_items sti ON sti.transfer_id = st.id
            WHERE st.destination_agreement_id = ? AND sti.item_id = ? AND st.status = 'POSTED'
            """, (rs, rowNum) -> new Movement(
                rs.getDate(1).toLocalDate(),
                rs.getBigDecimal(2),
                "SITE_TRANSFER_IN",
                rs.getLong(3),
                rs.getString(4)
        ), agreement.getId(), itemId).forEach(m::add);

        // Exchanged-In Items
        jdbc.query("""
            SELECT ie.exchange_date, ie.actual_quantity, ie.id, ie.exchange_number 
            FROM item_exchange_records ie
            WHERE ie.agreement_id = ? AND ie.actual_item_id = ? AND ie.status = 'POSTED'
            """, (rs, rowNum) -> new Movement(
                rs.getDate(1).toLocalDate(),
                rs.getBigDecimal(2),
                "ITEM_EXCHANGE_IN",
                rs.getLong(3),
                rs.getString(4)
        ), agreement.getId(), itemId).forEach(m::add);

        // Opening Site Balances
        jdbc.query("""
            SELECT tx.transaction_date, tx.quantity, tx.id, 'OPENING_STOCK'
            FROM stock_transactions tx
            WHERE tx.site_id = ? AND tx.party_id = ? AND tx.item_id = ? 
              AND tx.transaction_type = 'OPENING_SITE_BALANCE' 
              AND tx.reversal_of_transaction_id IS NULL
            """, (rs, rowNum) -> new Movement(
                rs.getDate(1).toLocalDate(),
                rs.getBigDecimal(2),
                "OPENING_SITE_BALANCE",
                rs.getLong(3),
                rs.getString(4)
        ), agreement.getSite().getId(), agreement.getParty().getId(), itemId).forEach(m::add);

        m.sort(Comparator.comparing(x -> x.date));
        return m;
    }

    private List<Movement> getOutwards(Agreement agreement, Long itemId) {
        List<Movement> m = new ArrayList<>();

        // Returns
        jdbc.query("""
            SELECT rc.receive_date, 
                   (rci.good_returned_quantity + rci.damaged_returned_quantity + rci.lost_quantity + rci.extra_returned_quantity) as quantity, 
                   rc.id, rc.receiving_challan_number 
            FROM receiving_challans rc
            JOIN receiving_challan_items rci ON rci.receiving_challan_id = rc.id
            WHERE rc.agreement_id = ? AND rci.item_id = ? AND rc.status = 'POSTED'
            """, (rs, rowNum) -> new Movement(
                rs.getDate(1).toLocalDate(),
                rs.getBigDecimal(2),
                "RECEIVING_CHALLAN",
                rs.getLong(3),
                rs.getString(4)
        ), agreement.getId(), itemId).forEach(m::add);

        // Outward Site Transfers
        jdbc.query("""
            SELECT st.transfer_date, sti.quantity, st.id, st.transfer_number 
            FROM site_transfers st
            JOIN site_transfer_items sti ON sti.transfer_id = st.id
            WHERE st.source_agreement_id = ? AND sti.item_id = ? AND st.status = 'POSTED'
            """, (rs, rowNum) -> new Movement(
                rs.getDate(1).toLocalDate(),
                rs.getBigDecimal(2),
                "SITE_TRANSFER_OUT",
                rs.getLong(3),
                rs.getString(4)
        ), agreement.getId(), itemId).forEach(m::add);

        // Manual Stock Losses
        jdbc.query("""
            SELECT sl.loss_date, sl.quantity, sl.id, sl.loss_number 
            FROM loss_records sl
            WHERE sl.agreement_id = ? AND sl.item_id = ? 
              AND sl.source_type = 'MANUAL_SITE_DECLARATION' AND sl.status = 'APPROVED'
            """, (rs, rowNum) -> new Movement(
                rs.getDate(1).toLocalDate(),
                rs.getBigDecimal(2),
                "STOCK_LOSS",
                rs.getLong(3),
                rs.getString(4)
        ), agreement.getId(), itemId).forEach(m::add);

        // Manual Stock Damages
        jdbc.query("""
            SELECT sd.damage_date, sd.quantity, sd.id, sd.damage_number 
            FROM damage_records sd
            WHERE sd.agreement_id = ? AND sd.item_id = ? 
              AND sd.source_type = 'MANUAL_SITE_DECLARATION' 
              AND sd.status IN ('RECORDED', 'UNDER_REPAIR', 'REPAIRED', 'SCRAPPED')
            """, (rs, rowNum) -> new Movement(
                rs.getDate(1).toLocalDate(),
                rs.getBigDecimal(2),
                "STOCK_DAMAGE",
                rs.getLong(3),
                rs.getString(4)
        ), agreement.getId(), itemId).forEach(m::add);

        // Exchanged-Out Items
        jdbc.query("""
            SELECT ie.exchange_date, ie.expected_quantity, ie.id, ie.exchange_number 
            FROM item_exchange_records ie
            WHERE ie.agreement_id = ? AND ie.expected_item_id = ? AND ie.status = 'POSTED'
            """, (rs, rowNum) -> new Movement(
                rs.getDate(1).toLocalDate(),
                rs.getBigDecimal(2),
                "ITEM_EXCHANGE_OUT",
                rs.getLong(3),
                rs.getString(4)
        ), agreement.getId(), itemId).forEach(m::add);

        m.sort(Comparator.comparing(x -> x.date));
        return m;
    }

    public void calculateTotals(BillingRun b) {
        BigDecimal subtotal = b.getSegments().stream()
                .map(BillingRunSegment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal discount = switch (b.getDiscountType()) {
            case NONE -> BigDecimal.ZERO;
            case PERCENTAGE -> {
                if (b.getDiscountValue().compareTo(HUNDRED) > 0) {
                    throw new IllegalArgumentException("Percentage discount cannot exceed 100");
                }
                yield subtotal.multiply(b.getDiscountValue()).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            }
            case FIXED -> {
                if (b.getDiscountValue().compareTo(subtotal) > 0) {
                    throw new IllegalArgumentException("Fixed discount cannot exceed subtotal");
                }
                yield b.getDiscountValue().setScale(2, RoundingMode.HALF_UP);
            }
        };

        BigDecimal losses = b.getCharges().stream()
                .filter(c -> c.isSelected() && "LOSS_RECOVERY".equals(c.getChargeType()))
                .map(BillingRunCharge::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal damages = b.getCharges().stream()
                .filter(c -> c.isSelected() && ("DAMAGE_RECOVERY".equals(c.getChargeType()) || "REPAIR_CHARGE".equals(c.getChargeType())))
                .map(BillingRunCharge::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal operational = b.getCharges().stream()
                .filter(c -> c.isSelected() && !List.of("LOSS_RECOVERY", "DAMAGE_RECOVERY", "REPAIR_CHARGE").contains(c.getChargeType()))
                .map(BillingRunCharge::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal adj = b.getManualAdjustmentTotal().setScale(2, RoundingMode.HALF_UP);

        BigDecimal taxable = subtotal.subtract(discount)
                .add(losses)
                .add(damages)
                .add(operational)
                .add(adj)
                .setScale(2, RoundingMode.HALF_UP);

        if (taxable.signum() < 0) {
            throw new IllegalArgumentException("Total taxable amount cannot be negative");
        }

        BigDecimal cgst = percent(taxable, b.getCgstRate());
        BigDecimal sgst = percent(taxable, b.getSgstRate());
        BigDecimal igst = percent(taxable, b.getIgstRate());
        BigDecimal tax = cgst.add(sgst).add(igst).setScale(2, RoundingMode.HALF_UP);

        b.setRentalSubtotal(subtotal);
        b.setDiscountAmount(discount);
        b.setLossChargeTotal(losses);
        b.setDamageChargeTotal(damages);
        b.setOperationalChargeTotal(operational);
        b.setTaxableAmount(taxable);
        b.setCgstAmount(cgst);
        b.setSgstAmount(sgst);
        b.setIgstAmount(igst);
        b.setTotalTax(tax);

        BigDecimal rawGrand = taxable.add(tax);
        BigDecimal rounded = rawGrand.setScale(0, RoundingMode.HALF_UP);
        BigDecimal roundOff = rounded.subtract(rawGrand).setScale(2, RoundingMode.HALF_UP);

        b.setRoundOff(roundOff);
        b.setGrandTotal(rounded.setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal percent(BigDecimal base, BigDecimal rate) {
        return base.multiply(rate).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}

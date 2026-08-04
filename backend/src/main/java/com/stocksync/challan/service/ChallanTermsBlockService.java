package com.stocksync.challan.service;

import com.stocksync.agreement.entity.Agreement;
import com.stocksync.agreement.entity.AgreementItem;
import com.stocksync.agreement.repository.AgreementRepository;
import com.stocksync.order.repository.SiteOrderRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChallanTermsBlockService {
    private final AgreementRepository agreements;
    private final SiteOrderRepository orders;

    public ChallanTermsBlockService(AgreementRepository agreements, SiteOrderRepository orders) {
        this.agreements = agreements;
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public List<String> forSiteOrder(Long siteOrderId) {
        if (siteOrderId == null) return List.of();
        return orders.findDetailedById(siteOrderId)
                .map(order -> order.getAgreement() == null ? null : order.getAgreement().getId())
                .flatMap(agreements::findDetailedById)
                .map(this::lines)
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<String> forAgreement(Long agreementId) {
        if (agreementId == null) return List.of();
        return agreements.findDetailedById(agreementId)
                .map(this::lines)
                .orElse(List.of());
    }

    private List<String> lines(Agreement agreement) {
        List<String> result = new ArrayList<>();
        result.add("Loss/damage rates as per quotation:");
        agreement.getItems().stream()
                .sorted(Comparator.comparingInt(AgreementItem::getSequence))
                .map(this::rateLine)
                .filter(line -> line != null && !line.isBlank())
                .limit(11)
                .forEach(result::add);
        if (result.size() == 1 && agreement.getTerms() != null && !agreement.getTerms().isBlank()) {
            splitTerms(agreement.getTerms()).stream().limit(12).forEach(result::add);
        }
        return result;
    }

    private String rateLine(AgreementItem item) {
        BigDecimal rate = firstPositive(
                item.getLossRatePerPiece(),
                item.getDamageRate(),
                item.getSourceQuotationItem() == null ? null : item.getSourceQuotationItem().getReplacementRate());
        if (rate == null) return null;
        return item.getItemNameSnapshot() + ":- Rs. " + money(rate) + "/- per " + unit(item.getUnitSnapshot());
    }

    private BigDecimal firstPositive(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value != null && value.compareTo(BigDecimal.ZERO) > 0) return value;
        }
        return null;
    }

    private String money(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private String unit(String value) {
        if (value == null || value.isBlank()) return "unit";
        String unit = value.trim().toLowerCase();
        if ("pcs".equals(unit) || "piece".equals(unit) || "pieces".equals(unit)) return "no.";
        return unit;
    }

    private List<String> splitTerms(String terms) {
        String normalized = terms.replace("\r", "\n").replaceAll("\\n+", "\n").trim();
        if (normalized.isBlank()) return List.of();
        return List.of(normalized.split("\\n"));
    }
}

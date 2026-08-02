package com.stocksync.quotation.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;

final class QuotationGstAllocation {
    private static final Map<String, String> STATE_CODES = Map.ofEntries(
            Map.entry("andhrapradesh", "37"), Map.entry("arunachalpradesh", "12"),
            Map.entry("assam", "18"), Map.entry("bihar", "10"), Map.entry("chhattisgarh", "22"),
            Map.entry("goa", "30"), Map.entry("gujarat", "24"), Map.entry("haryana", "06"),
            Map.entry("himachalpradesh", "02"), Map.entry("jharkhand", "20"), Map.entry("karnataka", "29"),
            Map.entry("kerala", "32"), Map.entry("madhyapradesh", "23"), Map.entry("maharashtra", "27"),
            Map.entry("manipur", "14"), Map.entry("meghalaya", "17"), Map.entry("mizoram", "15"),
            Map.entry("nagaland", "13"), Map.entry("odisha", "21"), Map.entry("punjab", "03"),
            Map.entry("rajasthan", "08"), Map.entry("sikkim", "11"), Map.entry("tamilnadu", "33"),
            Map.entry("telangana", "36"), Map.entry("tripura", "16"), Map.entry("uttarpradesh", "09"),
            Map.entry("uttarakhand", "05"), Map.entry("westbengal", "19"), Map.entry("delhi", "07"),
            Map.entry("jammuandkashmir", "01"), Map.entry("ladakh", "38"),
            Map.entry("chandigarh", "04"), Map.entry("puducherry", "34"));

    private QuotationGstAllocation() {}

    static Rates exact(BigDecimal totalRate, String companyGstin, String companyAddress,
                       String partyGstin, String partyState) {
        BigDecimal total = totalRate == null ? BigDecimal.ZERO : totalRate;
        String supplierState = stateCode(companyGstin, companyAddress);
        String customerState = stateCode(partyGstin, partyState);
        if (supplierState != null && supplierState.equals(customerState)) {
            BigDecimal half = total.divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP);
            return new Rates(half, half, BigDecimal.ZERO);
        }
        return new Rates(BigDecimal.ZERO, BigDecimal.ZERO, total);
    }

    private static String stateCode(String gstin, String location) {
        if (gstin != null && gstin.trim().matches("^[0-9]{2}[0-9A-Z]{13}$")) return gstin.trim().substring(0, 2);
        String normalized = location == null ? "" : location.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        return STATE_CODES.entrySet().stream()
                .filter(entry -> normalized.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
    }

    record Rates(BigDecimal cgst, BigDecimal sgst, BigDecimal igst) {}
}

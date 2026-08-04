package com.stocksync.common.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class PdfViewHelper {
    public static final PdfViewHelper INSTANCE = new PdfViewHelper();

    private static final DateTimeFormatter MONTH_DATE = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
    private static final String[] SMALL = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };
    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private PdfViewHelper() {
    }

    public String date(LocalDate date) {
        if (date == null) {
            return "-";
        }
        int day = date.getDayOfMonth();
        String suffix = (day >= 11 && day <= 13) ? "th" : switch (day % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
        return day + suffix + " " + date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
    }

    public String shortDate(LocalDate date) {
        return date == null ? "-" : date.format(MONTH_DATE);
    }

    public String amount(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public String qty(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    public BigDecimal lineAmount(BigDecimal quantity, BigDecimal rate, int months) {
        if (quantity == null || rate == null) {
            return BigDecimal.ZERO;
        }
        return quantity.multiply(rate).multiply(BigDecimal.valueOf(Math.max(months, 1)));
    }

    public int months(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            return 1;
        }
        Period period = Period.between(from, to);
        int months = period.getYears() * 12 + period.getMonths();
        if (period.getDays() > 0) {
            months++;
        }
        return Math.max(months, 1);
    }

    public String billingMonths(Integer billableDays, LocalDate from, LocalDate to) {
        if (billableDays == null || billableDays <= 0) {
            return String.valueOf(months(from, to));
        }
        return BigDecimal.valueOf(billableDays)
                .divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    public String amountInWords(BigDecimal value) {
        if (value == null) {
            return "Rupees Zero Only";
        }
        long whole = value.setScale(0, RoundingMode.HALF_UP).longValue();
        return "Rupees " + words(whole) + " Only";
    }

    public String paragraphs(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    public boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public String hireTerms(String value) {
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return """
                1. Definitions
                Owner / We / Us means SteelFab Scaffoldings & Engineering Private Limited. Hirer / You / Your means the client accepting this quotation or agreement. Scaffolding Materials means all items supplied on hire under this document.

                2. Ownership and Responsibility
                All scaffolding materials remain the property of SteelFab Scaffoldings & Engineering Private Limited. The hirer shall be responsible for the materials from delivery until return and final reconciliation.

                3. Hirer Responsibilities
                The hirer shall use the materials only for the stated site, keep the materials in safe custody, and return the same materials in usable condition, normal wear and tear excepted.

                4. Hire Period, Returns, Loss and Damage
                Minimum hire period shall apply as mutually agreed. Materials not returned, returned short, or returned damaged shall be charged at the applicable replacement or repair rates.

                5. Security and Advance Payment
                Security deposit and advance rent, wherever applicable, shall be payable before dispatch. Security deposit shall be adjusted or refunded after final reconciliation of all dues, shortages and damages.

                6. Invoicing and Payment Terms
                Hire invoices shall be raised as per the agreed billing cycle. Payment shall be made within the agreed due period from the invoice date.

                7. Transportation
                To and fro transportation, Mathadi union payments, taxes and local statutory payments shall be in the client's scope unless specifically mentioned otherwise.

                8. Delivery and Delays
                Delivery shall be subject to availability of materials, transport arrangement and site readiness. SteelFab shall not be responsible for delays caused by site restrictions, third-party transport, local authority issues or force majeure events.

                9. Cancellation
                Cancellation after confirmation may attract charges for preparation, loading, transport or other committed costs.

                10. Complete Contract Agreement
                This document, together with accepted commercial terms and signed challans, forms the complete understanding between SteelFab and the hirer for the materials supplied on hire.
                """.trim();
    }

    private String words(long value) {
        if (value == 0) {
            return "Zero";
        }
        List<Scale> scales = List.of(
                new Scale(10_000_000L, "Crore"),
                new Scale(100_000L, "Lakh"),
                new Scale(1_000L, "Thousand"),
                new Scale(100L, "Hundred")
        );
        StringBuilder out = new StringBuilder();
        long remaining = value;
        for (Scale scale : scales) {
            long part = remaining / scale.value();
            if (part > 0) {
                append(out, wordsBelowHundred((int) part) + " " + scale.label());
                remaining %= scale.value();
            }
        }
        if (remaining > 0) {
            append(out, wordsBelowHundred((int) remaining));
        }
        return out.toString();
    }

    private String wordsBelowHundred(int value) {
        if (value < 20) {
            return SMALL[value];
        }
        return TENS[value / 10] + (value % 10 == 0 ? "" : " " + SMALL[value % 10]);
    }

    private void append(StringBuilder out, String value) {
        if (!out.isEmpty()) {
            out.append(' ');
        }
        out.append(value);
    }

    private record Scale(long value, String label) {
    }
}

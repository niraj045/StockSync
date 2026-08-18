package com.stocksync.common.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
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

    public String documentDate(LocalDate date) {
        return date == null ? "-" : date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
    }

    public String compactDate(LocalDate date) {
        return date == null ? "-" : date.format(DateTimeFormatter.ofPattern("dd-MM-yy"));
    }

    public String amount(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    public String qty(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    public String money(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("##,##,##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(value == null ? BigDecimal.ZERO : value);
    }

    public BigDecimal otherCharges(BigDecimal taxableAmount, BigDecimal rentalSubtotal, BigDecimal discountAmount) {
        return zero(taxableAmount).add(zero(discountAmount)).subtract(zero(rentalSubtotal));
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
        return "";
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

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record Scale(long value, String label) {
    }
}

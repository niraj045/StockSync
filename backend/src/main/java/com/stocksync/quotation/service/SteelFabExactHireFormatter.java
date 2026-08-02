package com.stocksync.quotation.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class SteelFabExactHireFormatter {
    private static final String[] ONES = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
    private static final String[] TENS = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.ENGLISH);
    public String date(LocalDate value) { return value == null ? "" : value.format(DATE); }
    public String dated(LocalDate value) {
        if(value==null)return "";int day=value.getDayOfMonth();int mod100=day%100;
        String suffix=mod100>=11&&mod100<=13?"th":switch(day%10){case 1->"st";case 2->"nd";case 3->"rd";default->"th";};
        return day+suffix+value.format(DateTimeFormatter.ofPattern(" MMMM uuuu",Locale.ENGLISH));
    }
    public String money(BigDecimal value) { return indian(zero(value).setScale(2, RoundingMode.HALF_UP).toPlainString()); }
    public String quantity(BigDecimal value) { return indian(zero(value).stripTrailingZeros().toPlainString()); }
    public String integerWords(int value) { return words(value); }

    public String amountInWords(BigDecimal value) {
        BigDecimal rounded = zero(value).setScale(2, RoundingMode.HALF_UP);
        long rupees = rounded.longValue();
        int paise = rounded.remainder(BigDecimal.ONE).movePointRight(2).intValue();
        String result = "INR " + words(rupees);
        if (paise > 0) result += " and " + words(paise) + " Paise";
        return result + " Only";
    }

    private String words(long number) {
        if (number == 0) return "Zero";
        if (number < 0) return "Minus " + words(-number);
        StringBuilder out = new StringBuilder();
        append(out, number / 10000000, "Crore"); number %= 10000000;
        long lacs=number/100000;append(out,lacs,lacs==1?"Lac":"Lacs");number%=100000;
        append(out, number / 1000, "Thousand"); number %= 1000;
        append(out, number / 100, "Hundred"); number %= 100;
        if (number > 0) { if (!out.isEmpty()) out.append(' '); out.append(belowHundred((int) number)); }
        return out.toString();
    }

    private void append(StringBuilder out, long value, String unit) {
        if (value == 0) return;
        if (!out.isEmpty()) out.append(' ');
        out.append(words(value)).append(' ').append(unit);
    }

    private String belowHundred(int number) {
        if (number < 20) return ONES[number];
        return TENS[number / 10] + (number % 10 == 0 ? "" : " " + ONES[number % 10]);
    }

    private String indian(String value) {
        boolean negative=value.startsWith("-");String unsigned=negative?value.substring(1):value;
        String[] parts=unsigned.split("\\.",2);String whole=parts[0];StringBuilder grouped=new StringBuilder();
        if(whole.length()<=3)grouped.append(whole);else{
            int last=whole.length()-3;String prefix=whole.substring(0,last);int first=prefix.length()%2;
            if(first>0)grouped.append(prefix,0,first);
            for(int i=first;i<prefix.length();i+=2){if(!grouped.isEmpty())grouped.append(',');grouped.append(prefix,i,i+2);}
            if(!grouped.isEmpty())grouped.append(',');grouped.append(whole.substring(last));
        }
        if(parts.length==2)grouped.append('.').append(parts[1]);
        return (negative?"-":"")+grouped;
    }

    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}

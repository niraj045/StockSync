package com.stocksync.quotation.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SteelFabExactHireFormatterTest {
    private final SteelFabExactHireFormatter formatter = new SteelFabExactHireFormatter();

    @Test void formatsIndianCurrencyAndWords() {
        assertThat(formatter.money(new BigDecimal("1514589"))).isEqualTo("15,14,589.00");
        assertThat(formatter.amountInWords(new BigDecimal("1514589")))
                .isEqualTo("INR Fifteen Lacs Fourteen Thousand Five Hundred Eighty Nine Only");
    }
}

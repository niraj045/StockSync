package com.stocksync.quotation.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class QuotationGstAllocationTest {
    @Test
    void splitsGstForSameStateUsingAddressFallback() {
        var rates = QuotationGstAllocation.exact(new BigDecimal("18"), null, "Mumbai, Maharashtra", null, "Maharashtra");
        assertThat(rates.cgst()).isEqualByComparingTo("9");
        assertThat(rates.sgst()).isEqualByComparingTo("9");
        assertThat(rates.igst()).isZero();
    }

    @Test
    void usesIgstForInterstateCustomerGstin() {
        var rates = QuotationGstAllocation.exact(new BigDecimal("18"), "27AAAAA0000A1Z5", null, "29AAAAA0000A1Z5", null);
        assertThat(rates.cgst()).isZero();
        assertThat(rates.sgst()).isZero();
        assertThat(rates.igst()).isEqualByComparingTo("18");
    }

    @Test
    void conservativelyUsesIgstWhenCustomerStateIsUnknown() {
        var rates = QuotationGstAllocation.exact(new BigDecimal("18"), null, "Mumbai, Maharashtra", null, null);
        assertThat(rates.igst()).isEqualByComparingTo("18");
    }
}

package com.stocksync.common.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PdfViewHelperTest {
    @Test
    void preservesFractionalThirtyDayBillingPeriodsUsedBySteelFabInvoices() {
        LocalDate from = LocalDate.of(2025, 12, 17);
        LocalDate to = LocalDate.of(2026, 5, 31);

        assertThat(PdfViewHelper.INSTANCE.billingMonths(165, from, to)).isEqualTo("5.5");
        assertThat(PdfViewHelper.INSTANCE.billingMonths(60, from, to)).isEqualTo("2");
    }
}

package com.stocksync.billing.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BillingRunCalculationServiceTest {

    @Test
    void fullCalendarMonthIsOneRentalMonth() {
        assertThat(BillingRunCalculationService.monthlyProrationFactor(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                .isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void partialCalendarMonthUsesActualDaysInThatMonth() {
        assertThat(BillingRunCalculationService.monthlyProrationFactor(
                LocalDate.of(2026, 8, 16), LocalDate.of(2026, 8, 31)))
                .isEqualByComparingTo(new BigDecimal("0.5161290323"));
    }

    @Test
    void periodAcrossMonthsProratesEachCalendarMonthSeparately() {
        assertThat(BillingRunCalculationService.monthlyProrationFactor(
                LocalDate.of(2026, 7, 29), LocalDate.of(2026, 8, 28)))
                .isEqualByComparingTo(new BigDecimal("1.0000000000"));
    }

    @Test
    void sevenDaysIsOneRentalWeek() {
        assertThat(BillingRunCalculationService.weeklyProrationFactor(7))
                .isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void partialWeekIsProratedBySevenDays() {
        assertThat(BillingRunCalculationService.weeklyProrationFactor(3))
                .isEqualByComparingTo(new BigDecimal("0.42857143"));
    }
}

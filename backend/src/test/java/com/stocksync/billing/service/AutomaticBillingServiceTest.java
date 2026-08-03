package com.stocksync.billing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.stocksync.agreement.entity.BillingCycle;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AutomaticBillingServiceTest {
    private static final LocalDate START = LocalDate.of(2026, 8, 3);

    @Test
    void weeklyPeriodContainsSevenCalendarDays() {
        assertThat(AutomaticBillingService.periodEnd(BillingCycle.WEEKLY, null, START))
                .isEqualTo(LocalDate.of(2026, 8, 9));
    }

    @Test
    void monthlyPeriodEndsOneDayBeforeSameDateNextMonth() {
        assertThat(AutomaticBillingService.periodEnd(BillingCycle.MONTHLY, null, START))
                .isEqualTo(LocalDate.of(2026, 9, 2));
    }

    @Test
    void customPeriodUsesConfiguredInclusiveDayCount() {
        assertThat(AutomaticBillingService.periodEnd(BillingCycle.CUSTOM, 10, START))
                .isEqualTo(LocalDate.of(2026, 8, 12));
    }
}

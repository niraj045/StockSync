package com.stocksync.billing.service;

import com.stocksync.agreement.entity.Agreement;
import com.stocksync.agreement.entity.AgreementStatus;
import com.stocksync.agreement.entity.BillingCommencementRule;
import com.stocksync.agreement.repository.AgreementRepository;
import com.stocksync.billing.dto.BillingRunDtos.BillingRunRequest;
import com.stocksync.billing.entity.BillingRun;
import com.stocksync.billing.entity.BillingRunStatus;
import com.stocksync.billing.repository.BillingRunRepository;
import com.stocksync.common.exception.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class AutomaticBillingService {
    private static final Logger log = LoggerFactory.getLogger(AutomaticBillingService.class);
    private static final int MAX_CATCH_UP_PERIODS = 120;

    private final AgreementRepository agreements;
    private final BillingRunRepository billingRuns;
    private final BillingRunService billingRunService;
    private final JdbcTemplate jdbc;
    private final boolean enabled;

    public AutomaticBillingService(
            AgreementRepository agreements,
            BillingRunRepository billingRuns,
            BillingRunService billingRunService,
            JdbcTemplate jdbc,
            @Value("${stocksync.billing.auto.enabled:true}") boolean enabled) {
        this.agreements = agreements;
        this.billingRuns = billingRuns;
        this.billingRunService = billingRunService;
        this.jdbc = jdbc;
        this.enabled = enabled;
    }

    @Scheduled(cron = "${stocksync.billing.auto.cron:0 15 2 * * *}", zone = "${APP_TIME_ZONE:Asia/Kolkata}")
    public void scheduledGeneration() {
        if (!enabled) return;
        GenerationSummary summary = generateDueDrafts(LocalDate.now());
        log.info("Automatic billing completed: generated={}, skipped={}, failed={}",
                summary.generated(), summary.skipped(), summary.failed());
    }

    public GenerationSummary generateDueDrafts(LocalDate asOf) {
        int generated = 0;
        int skipped = 0;
        List<String> failures = new ArrayList<>();

        for (Agreement agreement : agreements.findByStatus(AgreementStatus.ACTIVE)) {
            LocalDate start = nextStartDate(agreement);
            if (start == null) {
                skipped++;
                continue;
            }

            int catchUpCount = 0;
            while (catchUpCount++ < MAX_CATCH_UP_PERIODS) {
                LocalDate end = periodEnd(agreement.getBillingCycle(), agreement.getCustomBillingCycleDays(), start);
                if (end.isAfter(asOf) || (agreement.getExpiryDate() != null && start.isAfter(agreement.getExpiryDate()))) break;
                if (agreement.getExpiryDate() != null && end.isAfter(agreement.getExpiryDate())) end = agreement.getExpiryDate();

                try {
                    billingRunService.create(new BillingRunRequest(agreement.getId(), start, end), null);
                    generated++;
                    agreement.setLastAutoPeriodEnd(end);
                    agreement.setNextBillingDate(end.plusDays(1));
                    agreements.save(agreement);
                } catch (BusinessRuleException ex) {
                    if (!"OVERLAPPING_BILLING_PERIOD".equals(ex.getCode())) {
                        failures.add(agreement.getAgreementNumber() + ": " + ex.getMessage());
                        break;
                    }
                } catch (RuntimeException ex) {
                    failures.add(agreement.getAgreementNumber() + ": " + ex.getMessage());
                    break;
                }
                start = end.plusDays(1);
            }
        }
        return new GenerationSummary(generated, skipped, failures.size(), failures);
    }

    private LocalDate nextStartDate(Agreement agreement) {
        return billingRuns.findTopByAgreementIdAndStatusNotOrderByPeriodEndDesc(
                        agreement.getId(), BillingRunStatus.CANCELLED)
                .map(BillingRun::getPeriodEnd)
                .map(date -> date.plusDays(1))
                .orElseGet(() -> {
                    LocalDate configured = configuredStartDate(agreement);
                    if (configured == null) return null;
                    return configured.isBefore(agreement.getEffectiveDate()) ? agreement.getEffectiveDate() : configured;
                });
    }

    private LocalDate configuredStartDate(Agreement agreement) {
        BillingCommencementRule rule = agreement.getBillingCommencementRule() == null
                ? BillingCommencementRule.FIRST_DISPATCH : agreement.getBillingCommencementRule();
        return switch (rule) {
            case FIRST_DISPATCH -> firstDispatchDate(agreement.getId());
            case AGREEMENT_EFFECTIVE_DATE -> agreement.getEffectiveDate();
            case FIXED_DATE -> agreement.getFixedBillingStartDate();
        };
    }

    private LocalDate firstDispatchDate(Long agreementId) {
        return jdbc.query("""
                SELECT MIN(ic.dispatch_date)
                FROM issued_challans ic
                JOIN site_orders so ON so.id = ic.site_order_id
                WHERE so.agreement_id = ?
                """, rs -> rs.next() ? rs.getObject(1, LocalDate.class) : null, agreementId);
    }

    static LocalDate periodEnd(com.stocksync.agreement.entity.BillingCycle cycle, Integer customDays, LocalDate start) {
        return switch (cycle) {
            case WEEKLY -> start.plusDays(6);
            case MONTHLY -> start.plusMonths(1).minusDays(1);
            case CUSTOM -> start.plusDays(Math.max(1, customDays == null ? 30 : customDays) - 1L);
        };
    }

    public record GenerationSummary(int generated, int skipped, int failed, List<String> failures) {}
}

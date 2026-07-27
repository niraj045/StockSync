package com.stocksync.dashboard.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class DashboardDtos {
    private DashboardDtos() {}

    public record DashboardOverviewResponse(
            StockSummary stockSummary,
            MovementSummary movementSummary,
            OrderSummary orderSummary,
            ChallanSummary challanSummary,
            AgreementSummary agreementSummary,
            BillingSummary billingSummary,
            PaymentSummary paymentSummary,
            ExceptionSummary exceptionSummary,
            List<AttentionItem> attentionItems,
            List<ChartPoint> stockByStatus,
            List<TrendPoint> movementTrend,
            List<ChartPoint> topSites,
            List<ChartPoint> outstandingAgeing,
            List<DocumentItem> recentDocuments,
            List<ActivityItem> recentActivity,
            List<QuickAction> quickActions,
            String roleMode,
            Instant generatedAt
    ) {}

    public record StockSummary(
            BigDecimal godownAvailable,
            BigDecimal materialAtSites,
            BigDecimal damaged,
            BigDecimal lost,
            BigDecimal scrapped,
            BigDecimal underRepair,
            BigDecimal physicalCurrentStock,
            BigDecimal currentAccountableStock
    ) {}

    public record MovementSummary(
            BigDecimal issuedToday,
            BigDecimal receivedToday,
            BigDecimal issuedInPeriod,
            BigDecimal receivedInPeriod,
            long siteTransfersInPeriod
    ) {}

    public record OrderSummary(long openSiteOrders, long partiallyFulfilledSiteOrders, long fulfilledSiteOrders) {}

    public record ChallanSummary(long draftIssuedChallans, long draftReceivingChallans, long pendingExtraReturnApprovals) {}

    public record AgreementSummary(long activeAgreements, long expiringSoon, LocalDate warningDate) {}

    public record BillingSummary(long draftBillingRuns, long draftInvoices, long issuedInvoices, BigDecimal totalInvoicedInPeriod, BigDecimal totalOutstanding) {}

    public record PaymentSummary(BigDecimal cashReceived, BigDecimal tds, BigDecimal depositAdjustments, BigDecimal availableCustomerAdvance, BigDecimal availableSecurityDeposits) {}

    public record ExceptionSummary(
            long lossAwaitingApproval,
            long damageAwaitingAction,
            BigDecimal materialUnderRepair,
            long repairablePendingDecision,
            long partiallyFulfilledOrders,
            long extraReturnsAwaitingApproval,
            long lowStockMaterials,
            long overdueInvoices,
            long highOutstandingParties,
            long agreementsExpiringSoon
    ) {}

    public record AttentionItem(String key, String severity, String title, String description, String targetPath, long count) {}

    public record ChartPoint(String label, BigDecimal value) {}

    public record TrendPoint(LocalDate date, BigDecimal issued, BigDecimal received) {}

    public record DocumentItem(String type, Long id, String number, LocalDate documentDate, String status, String targetPath) {}

    public record ActivityItem(Long id, String action, String username, Instant createdAt, String description) {}

    public record QuickAction(String key, String label, String targetPath, String roleGroup) {}
}

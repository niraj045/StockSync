package com.stocksync.dashboard.service;

import com.stocksync.dashboard.dto.DashboardDtos.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DashboardOverviewService {
    private static final BigDecimal HIGH_OUTSTANDING_THRESHOLD = new BigDecimal("100000");
    private final NamedParameterJdbcTemplate jdbc;

    public DashboardOverviewService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public DashboardOverviewResponse overview(Long partyId, Long siteId, Long categoryId, LocalDate dateFrom, LocalDate dateTo, Authentication auth) {
        LocalDate to = dateTo == null ? LocalDate.now() : dateTo;
        LocalDate from = dateFrom == null ? to.minusDays(29) : dateFrom;
        LocalDate today = LocalDate.now();
        LocalDate expiringBy = today.plusDays(30);
        MapSqlParameterSource p = params(partyId, siteId, categoryId, from, to).addValue("today", today).addValue("expiringBy", expiringBy).addValue("highOutstanding", HIGH_OUTSTANDING_THRESHOLD);
        Set<String> roles = roles(auth);
        boolean financial = roles.contains("ROLE_ADMIN") || roles.contains("ROLE_ACCOUNTS");

        StockSummary stock = stockSummary(p);
        MovementSummary movement = movementSummary(p, today);
        OrderSummary orders = orderSummary(p);
        ChallanSummary challans = challanSummary(p);
        AgreementSummary agreements = agreementSummary(p, expiringBy);
        BillingSummary billing = financial ? billingSummary(p) : new BillingSummary(0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO);
        PaymentSummary payments = financial ? paymentSummary(p) : new PaymentSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        ExceptionSummary exceptions = exceptionSummary(p, orders.partiallyFulfilledSiteOrders(), agreements.expiringSoon());

        return new DashboardOverviewResponse(
                stock,
                movement,
                orders,
                challans,
                agreements,
                billing,
                payments,
                exceptions,
                attentionItems(exceptions, billing, agreements),
                stockByStatus(stock),
                movementTrend(p, from, to),
                topSites(p),
                financial ? outstandingAgeing(p, today) : List.of(),
                recentDocuments(p, financial),
                recentActivity(),
                quickActions(roles),
                financial ? "FINANCIAL" : roles.contains("ROLE_VIEWER") ? "READ_ONLY" : "OPERATIONS",
                Instant.now()
        );
    }

    private StockSummary stockSummary(MapSqlParameterSource p) {
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT COALESCE(SUM(sb.available_quantity),0) godown_available,
                       COALESCE((SELECT SUM(ssb.pending_quantity) FROM site_stock_balances ssb JOIN items si ON si.id=ssb.item_id WHERE ssb.pending_quantity > 0 AND (:siteId IS NULL OR ssb.site_id=:siteId) AND (:categoryId IS NULL OR si.category_id=:categoryId)),0) material_at_sites,
                       COALESCE(SUM(sb.damaged_quantity),0) damaged,
                       COALESCE(SUM(sb.lost_quantity),0) lost,
                       COALESCE(SUM(sb.scrapped_quantity),0) scrapped
                FROM stock_balances sb JOIN items i ON i.id=sb.item_id
                WHERE (:categoryId IS NULL OR i.category_id=:categoryId)
                """, p);
        BigDecimal godown = bd(row, "godown_available");
        BigDecimal atSites = bd(row, "material_at_sites");
        BigDecimal damaged = bd(row, "damaged");
        BigDecimal lost = bd(row, "lost");
        BigDecimal scrapped = bd(row, "scrapped");
        BigDecimal underRepair = one("SELECT COALESCE(SUM(quantity),0) FROM damage_records WHERE status='UNDER_REPAIR' AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId) AND (:categoryId IS NULL OR item_id IN (SELECT id FROM items WHERE category_id=:categoryId))", p);
        return new StockSummary(godown, atSites, damaged, lost, scrapped, underRepair, godown.add(atSites).add(damaged), godown.add(atSites).add(damaged).add(lost));
    }

    private MovementSummary movementSummary(MapSqlParameterSource p, LocalDate today) {
        MapSqlParameterSource todayParams = copy(p).addValue("todayOnly", today);
        BigDecimal issuedToday = one("SELECT COALESCE(SUM(quantity),0) FROM stock_transactions WHERE direction='OUT' AND transaction_date=:todayOnly AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId) AND (:categoryId IS NULL OR item_id IN (SELECT id FROM items WHERE category_id=:categoryId))", todayParams);
        BigDecimal receivedToday = one("SELECT COALESCE(SUM(quantity),0) FROM stock_transactions WHERE direction='IN' AND transaction_date=:todayOnly AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId) AND (:categoryId IS NULL OR item_id IN (SELECT id FROM items WHERE category_id=:categoryId))", todayParams);
        BigDecimal issued = one("SELECT COALESCE(SUM(quantity),0) FROM stock_transactions WHERE direction='OUT' AND transaction_date BETWEEN :dateFrom AND :dateTo AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId) AND (:categoryId IS NULL OR item_id IN (SELECT id FROM items WHERE category_id=:categoryId))", p);
        BigDecimal received = one("SELECT COALESCE(SUM(quantity),0) FROM stock_transactions WHERE direction='IN' AND transaction_date BETWEEN :dateFrom AND :dateTo AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId) AND (:categoryId IS NULL OR item_id IN (SELECT id FROM items WHERE category_id=:categoryId))", p);
        long transfers = count("SELECT COUNT(*) FROM site_transfers WHERE status='POSTED' AND transfer_date BETWEEN :dateFrom AND :dateTo AND (:partyId IS NULL OR source_party_id=:partyId OR destination_party_id=:partyId) AND (:siteId IS NULL OR source_site_id=:siteId OR destination_site_id=:siteId)", p);
        return new MovementSummary(issuedToday, receivedToday, issued, received, transfers);
    }

    private OrderSummary orderSummary(MapSqlParameterSource p) {
        return new OrderSummary(
                count("SELECT COUNT(*) FROM site_orders WHERE status IN ('DRAFT','CONFIRMED') AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId)", p),
                count("SELECT COUNT(*) FROM site_orders WHERE status='PARTIALLY_FULFILLED' AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId)", p),
                count("SELECT COUNT(*) FROM site_orders WHERE status IN ('FULFILLED','COMPLETED') AND order_date BETWEEN :dateFrom AND :dateTo AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId)", p)
        );
    }

    private ChallanSummary challanSummary(MapSqlParameterSource p) {
        long draftReceiving = count("SELECT COUNT(*) FROM receiving_challans WHERE status='DRAFT' AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId)", p);
        long extra = count("SELECT COUNT(*) FROM receiving_challans rc JOIN receiving_challan_items rci ON rci.receiving_challan_id=rc.id WHERE rc.status='EXTRA_RETURN_PENDING' AND rci.extra_returned_quantity > 0 AND (:partyId IS NULL OR rc.party_id=:partyId) AND (:siteId IS NULL OR rc.site_id=:siteId)", p);
        return new ChallanSummary(0, draftReceiving, extra);
    }

    private AgreementSummary agreementSummary(MapSqlParameterSource p, LocalDate expiringBy) {
        long active = count("SELECT COUNT(*) FROM agreements WHERE status='ACTIVE' AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId)", p);
        long expiring = count("SELECT COUNT(*) FROM agreements WHERE status='ACTIVE' AND expiry_date IS NOT NULL AND expiry_date BETWEEN :today AND :expiringBy AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId)", p);
        return new AgreementSummary(active, expiring, expiringBy);
    }

    private BillingSummary billingSummary(MapSqlParameterSource p) {
        return new BillingSummary(
                count("SELECT COUNT(*) FROM billing_runs WHERE status='DRAFT' AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId)", p),
                count("SELECT COUNT(*) FROM invoices WHERE status='DRAFT' AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId)", p),
                count("SELECT COUNT(*) FROM invoices WHERE status='ISSUED' AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId)", p),
                one("SELECT COALESCE(SUM(grand_total),0) FROM invoices WHERE status <> 'CANCELLED' AND invoice_date BETWEEN :dateFrom AND :dateTo AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId)", p),
                one("SELECT COALESCE(SUM(outstanding_amount),0) FROM invoices WHERE status <> 'CANCELLED' AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId)", p)
        );
    }

    private PaymentSummary paymentSummary(MapSqlParameterSource p) {
        BigDecimal cash = one("SELECT COALESCE(SUM(cash_amount),0) FROM payment_receipts WHERE status='POSTED' AND payment_date BETWEEN :dateFrom AND :dateTo AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId)", p);
        BigDecimal tds = one("SELECT COALESCE(SUM(tds_amount),0) FROM payment_receipts WHERE status='POSTED' AND payment_date BETWEEN :dateFrom AND :dateTo AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId)", p);
        BigDecimal deposits = one("SELECT COALESCE(SUM(amount),0) FROM security_deposit_transactions WHERE status='POSTED' AND transaction_type='ADJUSTMENT_TO_INVOICE' AND transaction_date BETWEEN :dateFrom AND :dateTo AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId)", p);
        BigDecimal advance = one("SELECT COALESCE(SUM(unallocated_amount),0) FROM payment_receipts WHERE status='POSTED' AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId)", p);
        BigDecimal availableDeposits = one("""
                SELECT COALESCE(SUM(CASE WHEN transaction_type='RECEIPT' THEN amount WHEN transaction_type IN ('REFUND','ADJUSTMENT_TO_INVOICE') THEN -amount ELSE 0 END),0)
                FROM security_deposit_transactions WHERE status='POSTED' AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId)
                """, p);
        return new PaymentSummary(cash, tds, deposits, advance, availableDeposits);
    }

    private ExceptionSummary exceptionSummary(MapSqlParameterSource p, long partialOrders, long expiring) {
        long loss = count("SELECT COUNT(*) FROM loss_records WHERE status IN ('DRAFT','PENDING_APPROVAL') AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId)", p);
        long damage = count("SELECT COUNT(*) FROM damage_records WHERE status IN ('RECORDED','AWAITING_ACTION','UNDER_REPAIR') AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId)", p);
        BigDecimal repairQty = one("SELECT COALESCE(SUM(quantity),0) FROM damage_records WHERE status='UNDER_REPAIR' AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId)", p);
        long repairable = count("SELECT COUNT(*) FROM damage_records WHERE repairable=TRUE AND status IN ('RECORDED','AWAITING_ACTION') AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId)", p);
        long extra = count("SELECT COUNT(*) FROM receiving_challans WHERE status='EXTRA_RETURN_PENDING' AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId)", p);
        long low = count("SELECT COUNT(*) FROM stock_balances sb JOIN items i ON i.id=sb.item_id WHERE i.minimum_stock IS NOT NULL AND sb.available_quantity < i.minimum_stock AND (:categoryId IS NULL OR i.category_id=:categoryId)", p);
        long overdue = count("SELECT COUNT(*) FROM invoices WHERE status='ISSUED' AND due_date < :today AND outstanding_amount > 0 AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId)", p);
        long high = count("SELECT COUNT(*) FROM (SELECT party_id, SUM(outstanding_amount) amount FROM invoices WHERE status <> 'CANCELLED' AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId) GROUP BY party_id HAVING amount >= :highOutstanding) x", p);
        return new ExceptionSummary(loss, damage, repairQty, repairable, partialOrders, extra, low, overdue, high, expiring);
    }

    private List<AttentionItem> attentionItems(ExceptionSummary e, BillingSummary b, AgreementSummary a) {
        List<AttentionItem> items = new ArrayList<>();
        add(items, "low-stock", "warning", "Low-stock materials", "Items below configured minimum stock.", "/inventory?belowMinimum=true", e.lowStockMaterials());
        add(items, "partial-orders", "warning", "Partially fulfilled orders", "Orders still waiting for complete issue.", "/orders?status=PARTIALLY_FULFILLED", e.partiallyFulfilledOrders());
        add(items, "overdue-invoices", "danger", "Overdue invoices", "Issued invoices past due date with outstanding amount.", "/invoices?status=ISSUED", e.overdueInvoices());
        add(items, "damage-action", "warning", "Damaged material awaiting action", "Damage records needing repair, scrap, or billing decision.", "/stock-damages", e.damageAwaitingAction());
        add(items, "loss-approval", "danger", "Loss records awaiting approval", "Loss records not yet approved or reversed.", "/stock-losses", e.lossAwaitingApproval());
        add(items, "expiring-agreements", "warning", "Agreements expiring soon", "Active agreements expiring by " + a.warningDate() + ".", "/agreements", e.agreementsExpiringSoon());
        add(items, "high-outstanding", "danger", "High outstanding parties", "Parties above the local attention threshold.", "/outstanding", e.highOutstandingParties());
        add(items, "draft-billing", "info", "Draft billing runs", "Billing runs prepared but not finalized.", "/billing-runs?status=DRAFT", b.draftBillingRuns());
        return items;
    }

    private List<ChartPoint> stockByStatus(StockSummary s) {
        return List.of(new ChartPoint("Godown", s.godownAvailable()), new ChartPoint("At sites", s.materialAtSites()), new ChartPoint("Damaged", s.damaged()), new ChartPoint("Lost", s.lost()), new ChartPoint("Scrapped", s.scrapped()), new ChartPoint("Repair", s.underRepair()));
    }

    private List<TrendPoint> movementTrend(MapSqlParameterSource p, LocalDate from, LocalDate to) {
        return jdbc.query("""
                SELECT transaction_date d,
                       COALESCE(SUM(CASE WHEN direction='OUT' THEN quantity ELSE 0 END),0) issued,
                       COALESCE(SUM(CASE WHEN direction='IN' THEN quantity ELSE 0 END),0) received
                FROM stock_transactions
                WHERE transaction_date BETWEEN :dateFrom AND :dateTo AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId)
                GROUP BY transaction_date ORDER BY transaction_date
                """, p, (rs, n) -> new TrendPoint(rs.getDate("d").toLocalDate(), rs.getBigDecimal("issued"), rs.getBigDecimal("received")));
    }

    private List<ChartPoint> topSites(MapSqlParameterSource p) {
        return jdbc.query("""
                SELECT s.site_name label, SUM(ssb.pending_quantity) value
                FROM site_stock_balances ssb JOIN sites s ON s.id=ssb.site_id JOIN items i ON i.id=ssb.item_id
                WHERE ssb.pending_quantity > 0 AND (:partyId IS NULL OR s.party_id=:partyId) AND (:siteId IS NULL OR s.id=:siteId) AND (:categoryId IS NULL OR i.category_id=:categoryId)
                GROUP BY s.id, s.site_name ORDER BY value DESC LIMIT 8
                """, p, (rs, n) -> new ChartPoint(rs.getString("label"), rs.getBigDecimal("value")));
    }

    private List<ChartPoint> outstandingAgeing(MapSqlParameterSource p, LocalDate today) {
        return jdbc.query("""
                SELECT bucket label, SUM(amount) value FROM (
                  SELECT CASE
                    WHEN DATEDIFF(:today, due_date) <= 0 THEN 'Not due'
                    WHEN DATEDIFF(:today, due_date) <= 30 THEN '1-30'
                    WHEN DATEDIFF(:today, due_date) <= 60 THEN '31-60'
                    WHEN DATEDIFF(:today, due_date) <= 90 THEN '61-90'
                    ELSE '90+' END bucket,
                    outstanding_amount amount
                  FROM invoices WHERE status <> 'CANCELLED' AND outstanding_amount > 0 AND (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId)
                ) x GROUP BY bucket
                """, p, (rs, n) -> new ChartPoint(rs.getString("label"), rs.getBigDecimal("value")));
    }

    private List<DocumentItem> recentDocuments(MapSqlParameterSource p, boolean financial) {
        String invoiceUnion = financial ? " UNION ALL SELECT 'Invoice', id, invoice_number, invoice_date, status, CONCAT('/invoices/', id) FROM invoices WHERE (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId)" : "";
        return jdbc.query("""
                SELECT * FROM (
                  SELECT 'Issued challan' type, ic.id id, ic.challan_number number, ic.dispatch_date document_date, 'POSTED' status, CONCAT('/challans/issued') target_path FROM issued_challans ic JOIN site_orders so ON so.id=ic.site_order_id WHERE (:partyId IS NULL OR so.party_id=:partyId) AND (:siteId IS NULL OR so.site_id=:siteId)
                  UNION ALL SELECT 'Receiving challan', id, receiving_challan_number, receive_date, status, '/challans/receiving' FROM receiving_challans WHERE (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId)
                  UNION ALL SELECT 'Site order', id, order_number, order_date, status, '/orders' FROM site_orders WHERE (:partyId IS NULL OR party_id=:partyId) AND (:siteId IS NULL OR site_id=:siteId)
                  """ + invoiceUnion + """
                ) docs ORDER BY document_date DESC, id DESC LIMIT 10
                """, p, (rs, n) -> new DocumentItem(rs.getString("type"), rs.getLong("id"), rs.getString("number"), rs.getDate("document_date").toLocalDate(), rs.getString("status"), rs.getString("target_path")));
    }

    private List<ActivityItem> recentActivity() {
        return jdbc.query("SELECT id, action, username_snapshot, created_at, description FROM user_activity_logs ORDER BY created_at DESC LIMIT 10", new MapSqlParameterSource(),
                (rs, n) -> new ActivityItem(rs.getLong("id"), rs.getString("action"), rs.getString("username_snapshot"), rs.getTimestamp("created_at").toInstant(), rs.getString("description")));
    }

    private List<QuickAction> quickActions(Set<String> roles) {
        if (roles.contains("ROLE_VIEWER")) return List.of();
        List<QuickAction> actions = new ArrayList<>();
        if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_OPERATIONS")) {
            actions.add(new QuickAction("quotation", "Create quotation", "/quotations/new", "operations"));
            actions.add(new QuickAction("order", "Create site order", "/orders", "operations"));
            actions.add(new QuickAction("issued", "Create issued challan", "/challans/issued", "operations"));
            actions.add(new QuickAction("receiving", "Create receiving challan", "/challans/receiving", "operations"));
            actions.add(new QuickAction("transfer", "Create site transfer", "/site-transfers", "operations"));
            actions.add(new QuickAction("loss", "Record loss", "/stock-losses", "operations"));
            actions.add(new QuickAction("damage", "Record damage", "/stock-damages", "operations"));
        }
        if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_ACCOUNTS")) {
            actions.add(new QuickAction("billing", "Create billing run", "/billing-runs", "accounts"));
            actions.add(new QuickAction("payment", "Record payment", "/payments", "accounts"));
        }
        return actions;
    }

    private MapSqlParameterSource params(Long partyId, Long siteId, Long categoryId, LocalDate from, LocalDate to) {
        return new MapSqlParameterSource().addValue("partyId", partyId).addValue("siteId", siteId).addValue("categoryId", categoryId).addValue("dateFrom", from).addValue("dateTo", to);
    }

    private MapSqlParameterSource copy(MapSqlParameterSource p) {
        MapSqlParameterSource copy = new MapSqlParameterSource();
        for (String name : p.getValues().keySet()) copy.addValue(name, p.getValue(name));
        return copy;
    }

    private BigDecimal one(String sql, MapSqlParameterSource p) {
        BigDecimal value = jdbc.queryForObject(sql, p, BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }

    private long count(String sql, MapSqlParameterSource p) {
        Long value = jdbc.queryForObject(sql, p, Long.class);
        return value == null ? 0 : value;
    }

    private BigDecimal bd(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value instanceof BigDecimal bd ? bd : value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }

    private void add(List<AttentionItem> items, String key, String severity, String title, String description, String target, long count) {
        if (count > 0) items.add(new AttentionItem(key, severity, title, description, target, count));
    }

    private Set<String> roles(Authentication auth) {
        if (auth == null) return Set.of("ROLE_VIEWER");
        return auth.getAuthorities().stream().map(a -> a.getAuthority().startsWith("ROLE_") ? a.getAuthority() : "ROLE_" + a.getAuthority()).collect(Collectors.toSet());
    }
}

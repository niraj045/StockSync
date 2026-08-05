package com.stocksync.reporting.service;

import com.stocksync.reporting.dto.ReportDtos.AgeingResponse;
import com.stocksync.reporting.dto.ReportDtos.ReportFilterRequest;
import com.stocksync.reporting.dto.ReportDtos.ReportPreviewResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportQueryService {
    private static final int DEFAULT_SIZE = 25;
    private static final int MAX_SIZE = 200;
    private final NamedParameterJdbcTemplate jdbc;
    private final ReportCatalogService catalog;

    public ReportQueryService(NamedParameterJdbcTemplate jdbc, ReportCatalogService catalog) {
        this.jdbc = jdbc;
        this.catalog = catalog;
    }

    @Transactional(readOnly = true)
    public ReportPreviewResponse preview(String reportType, ReportFilterRequest filters) {
        String type = catalog.normalize(reportType);
        QuerySpec spec = build(type, normalize(filters));
        int page = Math.max(0, filters != null && filters.page() != null ? filters.page() : 0);
        int size = Math.max(1, Math.min(MAX_SIZE, filters != null && filters.size() != null ? filters.size() : DEFAULT_SIZE));
        MapSqlParameterSource params = spec.params().addValue("limit", size).addValue("offset", page * size);
        long count = count(spec);
        List<Map<String, Object>> rows = jdbc.queryForList(spec.sql() + " LIMIT :limit OFFSET :offset", params).stream()
                .map(this::ordered)
                .toList();
        List<Map<String, Object>> totalRows = jdbc.queryForList(spec.sql(), spec.params()).stream().map(this::ordered).toList();
        return new ReportPreviewResponse(type, rows.isEmpty() ? spec.columns() : new ArrayList<>(rows.getFirst().keySet()), rows, totals(totalRows), page, size, count, spec.warning());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> exportRows(String reportType, ReportFilterRequest filters) {
        QuerySpec spec = build(catalog.normalize(reportType), normalize(filters));
        return jdbc.queryForList(spec.sql(), spec.params()).stream().map(this::ordered).toList();
    }

    @Transactional(readOnly = true)
    public AgeingResponse ageing(ReportFilterRequest request) {
        QuerySpec spec = build("OUTSTANDING_AGEING", normalize(request));
        List<Map<String, Object>> rows = jdbc.queryForList(spec.sql(), spec.params());
        BigDecimal current = BigDecimal.ZERO, d30 = BigDecimal.ZERO, d60 = BigDecimal.ZERO, d90 = BigDecimal.ZERO, older = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            current = current.add(bd(row.get("current")));
            d30 = d30.add(bd(row.get("days_1_to_30")));
            d60 = d60.add(bd(row.get("days_31_to_60")));
            d90 = d90.add(bd(row.get("days_61_to_90")));
            older = older.add(bd(row.get("more_than_90")));
        }
        return new AgeingResponse(current, d30, d60, d90, older, current.add(d30).add(d60).add(d90).add(older));
    }

    private QuerySpec build(String type, ReportFilterRequest f) {
        return switch (type) {
            case "CURRENT_STOCK_SUMMARY" -> currentStock(f);
            case "GODOWN_STOCK" -> godownStock(f);
            case "SITE_PENDING_STOCK" -> sitePendingStock(f);
            case "ITEM_LEDGER" -> itemLedger(f);
            case "LOW_STOCK" -> lowStock(f);
            case "DAMAGE_LOSS_SCRAP" -> damageLossScrap(f);
            case "PARTY_STOCK_SUMMARY" -> partyStock(f);
            case "SITE_STOCK_SUMMARY" -> siteStock(f);
            case "SITE_AGREEMENT_SUMMARY" -> siteAgreement(f);
            case "PARTY_OUTSTANDING" -> partyOutstanding(f);
            case "SITE_OUTSTANDING" -> siteOutstanding(f);
            case "MONTHLY_SITE_STATEMENT" -> monthlySiteStatement(f);
            case "SITE_ORDERS_REGISTER" -> siteOrders(f);
            case "ISSUED_CHALLANS_REGISTER" -> issuedChallans(f);
            case "RECEIVING_CHALLANS_REGISTER" -> receivingChallans(f);
            case "SITE_TRANSFERS_REGISTER" -> siteTransfers(f);
            case "SITE_OPERATIONS_REGISTER" -> siteOperations(f);
            case "LOSS_RECORDS_REGISTER" -> lossRecords(f);
            case "DAMAGE_RECORDS_REGISTER" -> damageRecords(f);
            case "ITEM_EXCHANGES_REGISTER" -> itemExchanges(f);
            case "PURCHASES_REGISTER" -> purchases(f);
            case "SCRAP_REGISTER" -> scrap(f);
            case "STOCK_ADJUSTMENTS_REGISTER" -> adjustments(f);
            case "QUOTATION_REGISTER" -> quotations(f);
            case "INQUIRY_REGISTER" -> inquiryRegister(f);
            case "AGREEMENT_REGISTER" -> agreements(f);
            case "BILLING_RUN_REGISTER" -> billingRuns(f);
            case "INVOICE_REGISTER" -> invoiceRegister(f);
            case "PAYMENT_REGISTER" -> payments(f);
            case "TDS_REPORT" -> tds(f);
            case "SECURITY_DEPOSIT_REPORT" -> securityDeposits(f);
            case "OUTSTANDING_AGEING" -> outstandingAgeing(f);
            case "PARTY_SITE_LEDGER" -> partySiteLedger(f);
            case "GST_SALES_REGISTER", "GSTR1_PREPARATION" -> gstSales(f);
            case "GST_TAX_SUMMARY", "GSTR3B_SUMMARY" -> gstTaxSummary(f);
            default -> throw new IllegalArgumentException("Unsupported report type");
        };
    }

    private QuerySpec currentStock(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT i.item_code item_code, i.item_name item, c.name category, i.unit unit,
                       COALESCE(sb.available_quantity,0) godown_available,
                       COALESCE(site.site_pending,0) at_sites,
                       COALESCE(sb.damaged_quantity,0) damaged,
                       COALESCE(sb.lost_quantity,0) lost,
                       COALESCE(sb.scrapped_quantity,0) scrapped,
                       COALESCE(sb.available_quantity,0) + COALESCE(site.site_pending,0) + COALESCE(sb.damaged_quantity,0) current_accountable_stock
                FROM items i
                LEFT JOIN item_categories c ON c.id = i.category_id
                LEFT JOIN stock_balances sb ON sb.item_id = i.id
                LEFT JOIN (SELECT item_id, SUM(pending_quantity) site_pending FROM site_stock_balances GROUP BY item_id) site ON site.item_id = i.id
                WHERE i.active = TRUE
                """);
        itemFilters(sql, f, "i", "c");
        sql.append(" ORDER BY i.item_name");
        return sql.spec();
    }

    private QuerySpec godownStock(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT i.item_code item_code, i.item_name item, c.name category, i.unit unit,
                       SUM(CASE WHEN st.direction='IN' THEN st.quantity ELSE 0 END) inward,
                       SUM(CASE WHEN st.direction='OUT' THEN st.quantity ELSE 0 END) outward,
                       SUM(CASE WHEN st.transaction_type='ADJUSTMENT' THEN st.quantity ELSE 0 END) adjustments,
                       COALESCE(sb.damaged_quantity,0) damaged,
                       COALESCE(sb.scrapped_quantity,0) scrap,
                       COALESCE(sb.available_quantity,0) closing_balance
                FROM items i
                LEFT JOIN item_categories c ON c.id = i.category_id
                LEFT JOIN stock_balances sb ON sb.item_id = i.id
                LEFT JOIN stock_transactions st ON st.item_id = i.id
                WHERE i.active = TRUE
                """);
        itemFilters(sql, f, "i", "c");
        date(sql, f, "st.transaction_date");
        sql.append(" GROUP BY i.id, c.name, sb.damaged_quantity, sb.scrapped_quantity, sb.available_quantity ORDER BY i.item_name");
        return sql.spec();
    }

    private QuerySpec sitePendingStock(ReportFilterRequest f) {
        Sql sql = new Sql("""
                WITH site_items AS (
                    SELECT site_id, item_id FROM site_stock_balances
                    UNION
                    SELECT so.site_id, ici.item_id FROM issued_challan_items ici JOIN issued_challans ic ON ic.id = ici.issued_challan_id JOIN site_orders so ON so.id = ic.site_order_id
                    UNION
                    SELECT rc.site_id, rci.item_id FROM receiving_challan_items rci JOIN receiving_challans rc ON rc.id = rci.receiving_challan_id
                    UNION
                    SELECT so.site_id, soi.item_id FROM site_order_items soi JOIN site_orders so ON so.id = soi.site_order_id
                    UNION
                    SELECT a.site_id, ai.item_id FROM agreement_items ai JOIN agreements a ON a.id = ai.agreement_id WHERE a.status <> 'CANCELLED'
                )
                SELECT p.legal_name party, s.site_name site, a.agreement_number agreement, i.item_code item_code, i.item_name item,
                       COALESCE(opening.opening_qty,0) opening_site_quantity,
                       COALESCE(issued.issued_qty,0) issued,
                       COALESCE(received.received_qty,0) received,
                       COALESCE(loss.lost_qty,0) lost,
                       COALESCE(damage.damaged_qty,0) damaged,
                       COALESCE(tin.transfer_in,0) transfers_in,
                       COALESCE(tout.transfer_out,0) transfers_out,
                       COALESCE(ssb.pending_quantity, (COALESCE(opening.opening_qty,0) + COALESCE(issued.issued_qty,0) - COALESCE(received.received_qty,0) - COALESCE(loss.lost_qty,0) - COALESCE(damage.damaged_qty,0) + COALESCE(tin.transfer_in,0) - COALESCE(tout.transfer_out,0))) closing_pending
                FROM site_items si
                JOIN sites s ON s.id = si.site_id
                JOIN parties p ON p.id = s.party_id
                JOIN items i ON i.id = si.item_id
                LEFT JOIN site_stock_balances ssb ON ssb.site_id = si.site_id AND ssb.item_id = si.item_id
                LEFT JOIN agreements a ON a.site_id = s.id AND a.status <> 'CANCELLED'
                LEFT JOIN (SELECT site_id,item_id,SUM(quantity) opening_qty FROM stock_transactions WHERE transaction_type='OPENING_STOCK' GROUP BY site_id,item_id) opening ON opening.site_id=s.id AND opening.item_id=i.id
                LEFT JOIN (SELECT so.site_id, ici.item_id, SUM(ici.quantity) issued_qty FROM issued_challan_items ici JOIN issued_challans ic ON ic.id=ici.issued_challan_id JOIN site_orders so ON so.id=ic.site_order_id GROUP BY so.site_id, ici.item_id) issued ON issued.site_id=s.id AND issued.item_id=i.id
                LEFT JOIN (SELECT site_id,item_id,SUM(good_returned_quantity) received_qty FROM receiving_challan_items rci JOIN receiving_challans rc ON rc.id=rci.receiving_challan_id WHERE rc.status='POSTED' GROUP BY site_id,item_id) received ON received.site_id=s.id AND received.item_id=i.id
                LEFT JOIN (SELECT site_id,item_id,SUM(quantity) lost_qty FROM loss_records WHERE status <> 'REVERSED' GROUP BY site_id,item_id) loss ON loss.site_id=s.id AND loss.item_id=i.id
                LEFT JOIN (SELECT site_id,item_id,SUM(quantity) damaged_qty FROM damage_records WHERE status <> 'REVERSED' GROUP BY site_id,item_id) damage ON damage.site_id=s.id AND damage.item_id=i.id
                LEFT JOIN (SELECT destination_site_id site_id,item_id,SUM(quantity) transfer_in FROM site_transfer_items sti JOIN site_transfers st ON st.id=sti.transfer_id WHERE st.status='POSTED' GROUP BY destination_site_id,item_id) tin ON tin.site_id=s.id AND tin.item_id=i.id
                LEFT JOIN (SELECT source_site_id site_id,item_id,SUM(quantity) transfer_out FROM site_transfer_items sti JOIN site_transfers st ON st.id=sti.transfer_id WHERE st.status='POSTED' GROUP BY source_site_id,item_id) tout ON tout.site_id=s.id AND tout.item_id=i.id
                WHERE 1=1
                """);
        common(sql, f, "p.id", "s.id", "a.id", "i.id");
        sql.append(" ORDER BY p.legal_name, s.site_name, i.item_name");
        return sql.spec();
    }

    private QuerySpec itemLedger(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT st.transaction_date date, st.transaction_type transaction_type,
                       CONCAT(st.source_type, '/', st.source_id) reference_number,
                       COALESCE(s.site_name, 'Godown') location,
                       CASE WHEN st.direction='IN' THEN st.quantity ELSE 0 END in_quantity,
                       CASE WHEN st.direction='OUT' THEN st.quantity ELSE 0 END out_quantity,
                       SUM(CASE WHEN st.direction='IN' THEN st.quantity ELSE -st.quantity END)
                           OVER (PARTITION BY st.item_id ORDER BY st.transaction_date, st.id) running_balance,
                       st.created_by user
                FROM stock_transactions st
                LEFT JOIN sites s ON s.id = st.site_id
                WHERE 1=1
                """);
        common(sql, f, "st.party_id", "st.site_id", null, "st.item_id");
        date(sql, f, "st.transaction_date");
        if (has(f.user())) sql.eq("st.created_by", "user", f.user());
        sql.append(" ORDER BY st.transaction_date, st.id");
        return sql.spec();
    }

    private QuerySpec lowStock(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT i.item_code item_code, i.item_name item, c.name category, i.unit unit,
                       i.minimum_stock minimum_stock, COALESCE(sb.available_quantity,0) godown_available,
                       i.minimum_stock - COALESCE(sb.available_quantity,0) shortfall
                FROM items i
                LEFT JOIN item_categories c ON c.id=i.category_id
                LEFT JOIN stock_balances sb ON sb.item_id=i.id
                WHERE i.active=TRUE AND i.minimum_stock IS NOT NULL AND COALESCE(sb.available_quantity,0) < i.minimum_stock
                """);
        itemFilters(sql, f, "i", "c");
        sql.append(" ORDER BY shortfall DESC, i.item_name");
        return sql.spec();
    }

    private QuerySpec damageLossScrap(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT 'LOSS' record_type, lr.loss_number document_number, lr.loss_date date, p.legal_name party, s.site_name site, i.item_name item,
                       lr.quantity quantity, lr.weight weight, lr.status status, lr.calculated_recovery_amount charges, lr.source_type source_reference
                FROM loss_records lr JOIN parties p ON p.id=lr.party_id JOIN sites s ON s.id=lr.site_id JOIN items i ON i.id=lr.item_id
                WHERE 1=1
                """);
        common(sql, f, "lr.party_id", "lr.site_id", "lr.agreement_id", "lr.item_id");
        date(sql, f, "lr.loss_date");
        status(sql, f, "lr.status");
        doc(sql, f, "lr.loss_number");
        sql.append("""

                UNION ALL
                SELECT 'DAMAGE', dr.damage_number, dr.damage_date, p.legal_name, s.site_name, i.item_name,
                       dr.quantity, dr.weight, dr.status, dr.calculated_damage_amount, dr.source_type
                FROM damage_records dr JOIN parties p ON p.id=dr.party_id JOIN sites s ON s.id=dr.site_id JOIN items i ON i.id=dr.item_id
                WHERE 1=1
                """);
        common(sql, f, "dr.party_id", "dr.site_id", "dr.agreement_id", "dr.item_id");
        date(sql, f, "dr.damage_date");
        status(sql, f, "dr.status");
        doc(sql, f, "dr.damage_number");
        sql.append("""

                UNION ALL
                SELECT 'SCRAP', se.scrap_number, se.scrap_date, NULL, NULL, i.item_name,
                       si.quantity, NULL, 'POSTED', 0, se.reason
                FROM scrap_items si JOIN scrap_entries se ON se.id=si.scrap_entry_id JOIN items i ON i.id=si.item_id
                WHERE 1=1
                """);
        common(sql, f, null, null, null, "si.item_id");
        date(sql, f, "se.scrap_date");
        doc(sql, f, "se.scrap_number");
        sql.append(" ORDER BY date DESC, document_number");
        return sql.spec();
    }

    private QuerySpec partyStock(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT p.legal_name party, SUM(ssb.pending_quantity) pending_quantity, COUNT(DISTINCT ssb.site_id) site_count
                FROM site_stock_balances ssb JOIN sites s ON s.id=ssb.site_id JOIN parties p ON p.id=s.party_id WHERE 1=1
                """);
        common(sql, f, "p.id", "s.id", null, "ssb.item_id");
        sql.append(" GROUP BY p.id, p.legal_name ORDER BY p.legal_name");
        return sql.spec();
    }

    private QuerySpec siteStock(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT p.legal_name party, s.site_name site, s.status status, SUM(ssb.pending_quantity) pending_quantity, COUNT(DISTINCT ssb.item_id) item_count
                FROM site_stock_balances ssb JOIN sites s ON s.id=ssb.site_id JOIN parties p ON p.id=s.party_id WHERE 1=1
                """);
        common(sql, f, "p.id", "s.id", null, "ssb.item_id");
        status(sql, f, "s.status");
        sql.append(" GROUP BY p.legal_name, s.id, s.site_name, s.status ORDER BY p.legal_name, s.site_name");
        return sql.spec();
    }

    private QuerySpec siteAgreement(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT p.legal_name party, s.site_name site, a.agreement_number agreement, a.status status,
                       a.start_date start_date, a.end_date end_date, a.security_deposit required_deposit,
                       COALESCE(dep.available,0) available_deposit
                FROM agreements a JOIN parties p ON p.id=a.party_id JOIN sites s ON s.id=a.site_id
                LEFT JOIN (
                  SELECT agreement_id, SUM(CASE WHEN status='POSTED' AND transaction_type='RECEIPT' THEN amount WHEN status='POSTED' AND transaction_type IN ('REFUND','ADJUSTMENT_TO_INVOICE') THEN -amount ELSE 0 END) available
                  FROM security_deposit_transactions GROUP BY agreement_id
                ) dep ON dep.agreement_id=a.id
                WHERE 1=1
                """);
        common(sql, f, "a.party_id", "a.site_id", "a.id", null);
        status(sql, f, "a.status");
        date(sql, f, "a.start_date");
        doc(sql, f, "a.agreement_number");
        sql.append(" ORDER BY a.start_date DESC, a.agreement_number");
        return sql.spec();
    }

    private QuerySpec partyOutstanding(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT p.legal_name party, SUM(i.grand_total) total_billed, SUM(i.cash_allocated_total) cash_received,
                       SUM(i.tds_allocated_total) tds, SUM(i.deposit_adjusted_total) deposit_adjustments, SUM(i.outstanding_amount) outstanding
                FROM invoices i JOIN parties p ON p.id=i.party_id
                WHERE i.status <> 'CANCELLED'
                """);
        common(sql, f, "i.party_id", "i.site_id", "i.agreement_id", null);
        date(sql, f, "i.invoice_date");
        sql.append(" GROUP BY p.id, p.legal_name ORDER BY outstanding DESC");
        return sql.spec();
    }

    private QuerySpec siteOutstanding(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT p.legal_name party, s.site_name site, SUM(i.grand_total) total_billed, SUM(i.cash_allocated_total) cash_received,
                       SUM(i.tds_allocated_total) tds, SUM(i.deposit_adjusted_total) deposit_adjustments, SUM(i.outstanding_amount) outstanding
                FROM invoices i JOIN parties p ON p.id=i.party_id JOIN sites s ON s.id=i.site_id
                WHERE i.status <> 'CANCELLED'
                """);
        common(sql, f, "i.party_id", "i.site_id", "i.agreement_id", null);
        date(sql, f, "i.invoice_date");
        sql.append(" GROUP BY p.legal_name, s.id, s.site_name ORDER BY outstanding DESC");
        return sql.spec();
    }

    private QuerySpec monthlySiteStatement(ReportFilterRequest f) {
        ReportFilterRequest m = monthFilter(f);
        Sql sql = new Sql("""
                SELECT metric, amount FROM (
                  SELECT 'Opening pending' metric, COALESCE(SUM(CASE WHEN transaction_date < :startDate AND direction='OUT' THEN quantity WHEN transaction_date < :startDate AND direction='IN' THEN -quantity ELSE 0 END),0) amount FROM stock_transactions WHERE site_id=:siteId
                  UNION ALL SELECT 'Issued during period', COALESCE(SUM(quantity),0) FROM stock_transactions WHERE site_id=:siteId AND direction='OUT' AND transaction_date BETWEEN :startDate AND :endDate
                  UNION ALL SELECT 'Received during period', COALESCE(SUM(quantity),0) FROM stock_transactions WHERE site_id=:siteId AND direction='IN' AND transaction_date BETWEEN :startDate AND :endDate
                  UNION ALL SELECT 'Lost', COALESCE(SUM(quantity),0) FROM loss_records WHERE site_id=:siteId AND status <> 'REVERSED' AND loss_date BETWEEN :startDate AND :endDate
                  UNION ALL SELECT 'Damaged', COALESCE(SUM(quantity),0) FROM damage_records WHERE site_id=:siteId AND status <> 'REVERSED' AND damage_date BETWEEN :startDate AND :endDate
                  UNION ALL SELECT 'Closing pending', COALESCE(SUM(pending_quantity),0) FROM site_stock_balances WHERE site_id=:siteId
                  UNION ALL SELECT 'Invoices', COALESCE(SUM(grand_total),0) FROM invoices WHERE site_id=:siteId AND status <> 'CANCELLED' AND invoice_date BETWEEN :startDate AND :endDate
                  UNION ALL SELECT 'Payments', COALESCE(SUM(cash_amount),0) FROM payment_receipts WHERE site_id=:siteId AND status='POSTED' AND payment_date BETWEEN :startDate AND :endDate
                  UNION ALL SELECT 'TDS', COALESCE(SUM(tds_amount),0) FROM payment_receipts WHERE site_id=:siteId AND status='POSTED' AND payment_date BETWEEN :startDate AND :endDate
                  UNION ALL SELECT 'Deposit adjustments', COALESCE(SUM(amount),0) FROM security_deposit_transactions WHERE site_id=:siteId AND status='POSTED' AND transaction_type='ADJUSTMENT_TO_INVOICE' AND transaction_date BETWEEN :startDate AND :endDate
                  UNION ALL SELECT 'Outstanding', COALESCE(SUM(outstanding_amount),0) FROM invoices WHERE site_id=:siteId AND status <> 'CANCELLED'
                ) statement_rows
                """);
        sql.params.addValue("siteId", m.siteId()).addValue("startDate", m.startDate()).addValue("endDate", m.endDate());
        return sql.spec("Select a site and month for the statement.");
    }

    private QuerySpec siteOrders(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT so.order_number document_number, so.order_date date, p.legal_name party, s.site_name site, so.status status,
                       SUM(soi.ordered_quantity) ordered_quantity, COALESCE(SUM(issued.issued_quantity),0) issued_quantity,
                       SUM(soi.ordered_quantity) - COALESCE(SUM(issued.issued_quantity),0) remaining_quantity
                FROM site_orders so JOIN site_order_items soi ON soi.order_id=so.id JOIN parties p ON p.id=so.party_id JOIN sites s ON s.id=so.site_id
                LEFT JOIN (SELECT ic.site_order_id, ici.item_id, SUM(ici.quantity) issued_quantity FROM issued_challans ic JOIN issued_challan_items ici ON ici.issued_challan_id=ic.id GROUP BY ic.site_order_id, ici.item_id) issued ON issued.site_order_id=so.id AND issued.item_id=soi.item_id
                WHERE 1=1
                """);
        common(sql, f, "so.party_id", "so.site_id", "so.agreement_id", "soi.item_id");
        date(sql, f, "so.order_date");
        status(sql, f, "so.status");
        doc(sql, f, "so.order_number");
        sql.append(" GROUP BY so.id, so.order_number, so.order_date, p.legal_name, s.site_name, so.status ORDER BY so.order_date DESC");
        return sql.spec();
    }

    private QuerySpec issuedChallans(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT ic.challan_number document_number, ic.dispatch_date date, p.legal_name party, s.site_name site,
                       ic.vehicle_number vehicle, ic.driver_name driver, SUM(ici.quantity) issued_quantity, SUM(ici.quantity * COALESCE(i.weight_per_piece,0)) weight, so.status status
                FROM issued_challans ic JOIN site_orders so ON so.id=ic.site_order_id JOIN parties p ON p.id=so.party_id JOIN sites s ON s.id=so.site_id
                JOIN issued_challan_items ici ON ici.issued_challan_id=ic.id JOIN items i ON i.id=ici.item_id WHERE 1=1
                """);
        common(sql, f, "so.party_id", "so.site_id", "so.agreement_id", "ici.item_id");
        date(sql, f, "ic.dispatch_date");
        status(sql, f, "so.status");
        doc(sql, f, "ic.challan_number");
        sql.append(" GROUP BY ic.id, ic.challan_number, ic.dispatch_date, p.legal_name, s.site_name, ic.vehicle_number, ic.driver_name, so.status ORDER BY ic.dispatch_date DESC");
        return sql.spec();
    }

    private QuerySpec receivingChallans(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT rc.receiving_challan_number document_number, rc.receive_date date, p.legal_name party, s.site_name site,
                       rc.vehicle_number vehicle, rc.driver_name driver, rc.status status,
                       SUM(rci.good_returned_quantity) received_quantity, SUM(rci.damaged_returned_quantity) damaged_quantity, SUM(rci.lost_quantity) lost_quantity
                FROM receiving_challans rc JOIN receiving_challan_items rci ON rci.receiving_challan_id=rc.id JOIN parties p ON p.id=rc.party_id JOIN sites s ON s.id=rc.site_id WHERE 1=1
                """);
        common(sql, f, "rc.party_id", "rc.site_id", "rc.agreement_id", "rci.item_id");
        date(sql, f, "rc.receive_date");
        status(sql, f, "rc.status");
        doc(sql, f, "rc.receiving_challan_number");
        sql.append(" GROUP BY rc.id, rc.receiving_challan_number, rc.receive_date, p.legal_name, s.site_name, rc.vehicle_number, rc.driver_name, rc.status ORDER BY rc.receive_date DESC");
        return sql.spec();
    }

    private QuerySpec siteTransfers(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT st.transfer_number document_number, st.transfer_date date, sp.legal_name source_party, ss.site_name source_site,
                       dp.legal_name destination_party, ds.site_name destination_site, st.status status, st.vehicle_number vehicle,
                       SUM(sti.quantity) quantity, SUM(sti.total_weight) weight
                FROM site_transfers st JOIN site_transfer_items sti ON sti.transfer_id=st.id
                JOIN parties sp ON sp.id=st.source_party_id JOIN sites ss ON ss.id=st.source_site_id
                JOIN parties dp ON dp.id=st.destination_party_id JOIN sites ds ON ds.id=st.destination_site_id WHERE 1=1
                """);
        common(sql, f, "st.source_party_id", "st.source_site_id", "st.source_agreement_id", "sti.item_id");
        date(sql, f, "st.transfer_date");
        status(sql, f, "st.status");
        doc(sql, f, "st.transfer_number");
        sql.append(" GROUP BY st.id, st.transfer_number, st.transfer_date, sp.legal_name, ss.site_name, dp.legal_name, ds.site_name, st.status, st.vehicle_number ORDER BY st.transfer_date DESC");
        return sql.spec();
    }

    private QuerySpec siteOperations(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT o.operation_number document_number, o.operation_date date, o.operation_type type, o.direction direction,
                       p.legal_name party, s.site_name site, o.provider_type provider_type, o.provider_name provider,
                       o.transporter_name transporter, o.vehicle_number vehicle, o.driver_name driver, o.worker_count workers,
                       o.quantity quantity, o.rate rate, o.amount amount, o.charge_to_client charge_client, o.status status, o.reference_number ref_no
                FROM site_operations o LEFT JOIN parties p ON p.id=o.party_id JOIN sites s ON s.id=o.site_id WHERE 1=1
                """);
        common(sql, f, "o.party_id", "o.site_id", null, null);
        date(sql, f, "o.operation_date");
        status(sql, f, "o.status");
        doc(sql, f, "o.operation_number");
        sql.append(" ORDER BY o.operation_date DESC, o.id DESC");
        return sql.spec();
    }

    private QuerySpec inquiryRegister(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT i.inquiry_number document_number, i.inquiry_date date, i.source source, i.contact_name contact,
                       i.phone phone, i.email email, p.legal_name party, s.site_name site, i.requirement requirement,
                       i.follow_up_date follow_up_date, i.status status, q.quotation_number quote_no, i.notes notes
                FROM client_inquiries i LEFT JOIN parties p ON p.id=i.party_id LEFT JOIN sites s ON s.id=i.site_id
                LEFT JOIN quotations q ON q.id=i.quotation_id WHERE 1=1
                """);
        common(sql, f, "i.party_id", "i.site_id", null, null);
        date(sql, f, "i.inquiry_date");
        status(sql, f, "i.status");
        doc(sql, f, "i.inquiry_number");
        sql.append(" ORDER BY i.inquiry_date DESC, i.id DESC");
        return sql.spec();
    }

    private QuerySpec lossRecords(ReportFilterRequest f) { return simpleException(f, "loss_records", "loss_number", "loss_date", "calculated_recovery_amount"); }
    private QuerySpec damageRecords(ReportFilterRequest f) { return simpleException(f, "damage_records", "damage_number", "damage_date", "calculated_damage_amount"); }

    private QuerySpec simpleException(ReportFilterRequest f, String table, String number, String date, String charge) {
        Sql sql = new Sql(("SELECT r.%s document_number, r.%s date, p.legal_name party, s.site_name site, i.item_name item, r.quantity, r.weight, r.status, r.%s charges FROM %s r JOIN parties p ON p.id=r.party_id JOIN sites s ON s.id=r.site_id JOIN items i ON i.id=r.item_id WHERE 1=1 ")
                .formatted(number, date, charge, table));
        common(sql, f, "r.party_id", "r.site_id", "r.agreement_id", "r.item_id");
        date(sql, f, "r." + date);
        status(sql, f, "r.status");
        doc(sql, f, "r." + number);
        sql.append(" ORDER BY date DESC");
        return sql.spec();
    }

    private QuerySpec itemExchanges(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT er.exchange_number document_number, er.exchange_date date, p.legal_name party, s.site_name site,
                       expected.item_name expected_item, actual.item_name actual_item, er.expected_quantity, er.actual_quantity, er.status
                FROM item_exchange_records er JOIN parties p ON p.id=er.party_id JOIN sites s ON s.id=er.site_id
                JOIN items expected ON expected.id=er.expected_item_id JOIN items actual ON actual.id=er.actual_item_id WHERE 1=1
                """);
        common(sql, f, "er.party_id", "er.site_id", "er.agreement_id", "er.expected_item_id");
        date(sql, f, "er.exchange_date");
        status(sql, f, "er.status");
        doc(sql, f, "er.exchange_number");
        sql.append(" ORDER BY er.exchange_date DESC");
        return sql.spec();
    }

    private QuerySpec purchases(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT p.purchase_number document_number, p.purchase_date date, v.name vendor, i.item_name item, pi.quantity, pi.unit_rate, pi.line_value
                FROM purchases p JOIN vendors v ON v.id=p.vendor_id JOIN purchase_items pi ON pi.purchase_id=p.id JOIN items i ON i.id=pi.item_id WHERE 1=1
                """);
        common(sql, f, null, null, null, "pi.item_id");
        date(sql, f, "p.purchase_date");
        doc(sql, f, "p.purchase_number");
        sql.append(" ORDER BY p.purchase_date DESC");
        return sql.spec();
    }

    private QuerySpec scrap(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT se.scrap_number document_number, se.scrap_date date, i.item_name item, si.quantity, se.reason, se.created_by user
                FROM scrap_entries se JOIN scrap_items si ON si.scrap_entry_id=se.id JOIN items i ON i.id=si.item_id WHERE 1=1
                """);
        common(sql, f, null, null, null, "si.item_id");
        date(sql, f, "se.scrap_date");
        doc(sql, f, "se.scrap_number");
        if (has(f.user())) sql.eq("se.created_by", "user", f.user());
        sql.append(" ORDER BY se.scrap_date DESC");
        return sql.spec();
    }

    private QuerySpec adjustments(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT sa.adjustment_number document_number, sa.adjustment_date date, sa.direction status, i.item_name item, sai.quantity, sa.reason, sa.created_by user
                FROM stock_adjustments sa JOIN stock_adjustment_items sai ON sai.adjustment_id=sa.id JOIN items i ON i.id=sai.item_id WHERE 1=1
                """);
        common(sql, f, null, null, null, "sai.item_id");
        date(sql, f, "sa.adjustment_date");
        doc(sql, f, "sa.adjustment_number");
        status(sql, f, "sa.direction");
        if (has(f.user())) sql.eq("sa.created_by", "user", f.user());
        sql.append(" ORDER BY sa.adjustment_date DESC");
        return sql.spec();
    }

    private QuerySpec quotations(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT quotation_number document_number, quotation_date date, party_name_snapshot party, site_name_snapshot site, status,
                       subtotal, total_tax tax, grand_total, rental_type
                FROM quotations WHERE 1=1
                """);
        common(sql, f, "party_id", "site_id", null, null);
        date(sql, f, "quotation_date");
        status(sql, f, "status");
        doc(sql, f, "quotation_number");
        sql.append(" ORDER BY quotation_date DESC");
        return sql.spec();
    }

    private QuerySpec agreements(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT a.agreement_number document_number, a.agreement_date date, p.legal_name party, s.site_name site, a.status,
                       a.effective_date start_date, a.expiry_date end_date, a.security_deposit, a.billing_cycle
                FROM agreements a JOIN parties p ON p.id=a.party_id JOIN sites s ON s.id=a.site_id WHERE 1=1
                """);
        common(sql, f, "a.party_id", "a.site_id", "a.id", null);
        date(sql, f, "a.agreement_date");
        status(sql, f, "a.status");
        doc(sql, f, "a.agreement_number");
        sql.append(" ORDER BY a.agreement_date DESC");
        return sql.spec();
    }

    private QuerySpec billingRuns(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT br.billing_run_number document_number, br.period_start, br.period_end, p.legal_name party, s.site_name site, br.status,
                       br.rental_subtotal, br.loss_charge_total, br.damage_charge_total, br.operational_charge_total, br.taxable_amount, br.total_tax, br.grand_total
                FROM billing_runs br JOIN parties p ON p.id=br.party_id JOIN sites s ON s.id=br.site_id WHERE 1=1
                """);
        common(sql, f, "br.party_id", "br.site_id", "br.agreement_id", null);
        date(sql, f, "br.period_start");
        status(sql, f, "br.status");
        doc(sql, f, "br.billing_run_number");
        sql.append(" ORDER BY br.period_start DESC");
        return sql.spec();
    }

    private QuerySpec invoiceRegister(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT invoice_number document_number, invoice_date date, due_date, party_legal_name_snapshot party, site_name_snapshot site,
                       period_start, period_end, status, taxable_amount, cgst_amount cgst, sgst_amount sgst, igst_amount igst,
                       grand_total, cash_allocated_total cash, tds_allocated_total tds, deposit_adjusted_total deposit_adjusted,
                       outstanding_amount outstanding,
                       CASE WHEN outstanding_amount <= 0 THEN 'PAID' WHEN outstanding_amount < grand_total THEN 'PARTIALLY_PAID' ELSE 'UNPAID' END payment_status
                FROM invoices WHERE status <> 'CANCELLED'
                """);
        common(sql, f, "party_id", "site_id", "agreement_id", null);
        date(sql, f, "invoice_date");
        status(sql, f, "status");
        doc(sql, f, "invoice_number");
        sql.append(" ORDER BY invoice_date DESC");
        return sql.spec();
    }

    private QuerySpec payments(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT receipt_number document_number, payment_date date, party_name_snapshot party, site_name_snapshot site, payment_mode,
                       cash_amount, tds_amount, total_settlement_amount allocated_amount, unallocated_amount advance_remaining, status
                FROM payment_receipts WHERE 1=1
                """);
        common(sql, f, "party_id", "site_id", null, null);
        date(sql, f, "payment_date");
        status(sql, f, "status");
        doc(sql, f, "receipt_number");
        sql.append(" ORDER BY payment_date DESC");
        return sql.spec();
    }

    private QuerySpec tds(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT pr.party_name_snapshot party, i.invoice_number invoice, pr.receipt_number receipt, td.tds_amount,
                       td.section, td.certificate_number, td.verification_status, td.deduction_date
                FROM tds_details td JOIN payment_receipts pr ON pr.id=td.payment_receipt_id
                LEFT JOIN payment_allocations pa ON pa.payment_receipt_id=pr.id
                LEFT JOIN invoices i ON i.id=pa.invoice_id
                WHERE pr.status <> 'REVERSED'
                """);
        common(sql, f, "pr.party_id", "pr.site_id", null, null);
        date(sql, f, "pr.payment_date");
        status(sql, f, "td.verification_status");
        doc(sql, f, "pr.receipt_number");
        sql.append(" ORDER BY pr.payment_date DESC");
        return sql.spec();
    }

    private QuerySpec securityDeposits(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT a.agreement_number agreement, sdt.party_name_snapshot party, sdt.site_name_snapshot site, a.security_deposit required_deposit,
                       SUM(CASE WHEN sdt.status='POSTED' AND sdt.transaction_type='RECEIPT' THEN sdt.amount ELSE 0 END) received,
                       SUM(CASE WHEN sdt.status='POSTED' AND sdt.transaction_type='ADJUSTMENT_TO_INVOICE' THEN sdt.amount ELSE 0 END) adjusted,
                       SUM(CASE WHEN sdt.status='POSTED' AND sdt.transaction_type='REFUND' THEN sdt.amount ELSE 0 END) refunded,
                       SUM(CASE WHEN sdt.status='POSTED' AND sdt.transaction_type='RECEIPT' THEN sdt.amount WHEN sdt.status='POSTED' AND sdt.transaction_type IN ('REFUND','ADJUSTMENT_TO_INVOICE') THEN -sdt.amount ELSE 0 END) available,
                       SUM(CASE WHEN sdt.status='POSTED' AND sdt.transaction_type='RECEIPT' THEN sdt.amount WHEN sdt.status='POSTED' AND sdt.transaction_type IN ('REFUND','ADJUSTMENT_TO_INVOICE') THEN -sdt.amount ELSE 0 END) - a.security_deposit excess_shortfall
                FROM security_deposit_transactions sdt JOIN agreements a ON a.id=sdt.agreement_id WHERE 1=1
                """);
        common(sql, f, "sdt.party_id", "sdt.site_id", "sdt.agreement_id", null);
        date(sql, f, "sdt.transaction_date");
        sql.append(" GROUP BY a.agreement_number, sdt.party_name_snapshot, sdt.site_name_snapshot, a.security_deposit ORDER BY a.agreement_number");
        return sql.spec();
    }

    private QuerySpec outstandingAgeing(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT party_legal_name_snapshot party, site_name_snapshot site,
                       SUM(CASE WHEN due_date >= CURRENT_DATE THEN outstanding_amount ELSE 0 END) current,
                       SUM(CASE WHEN due_date < CURRENT_DATE AND due_date >= DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY) THEN outstanding_amount ELSE 0 END) days_1_to_30,
                       SUM(CASE WHEN due_date < DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY) AND due_date >= DATE_SUB(CURRENT_DATE, INTERVAL 60 DAY) THEN outstanding_amount ELSE 0 END) days_31_to_60,
                       SUM(CASE WHEN due_date < DATE_SUB(CURRENT_DATE, INTERVAL 60 DAY) AND due_date >= DATE_SUB(CURRENT_DATE, INTERVAL 90 DAY) THEN outstanding_amount ELSE 0 END) days_61_to_90,
                       SUM(CASE WHEN due_date < DATE_SUB(CURRENT_DATE, INTERVAL 90 DAY) THEN outstanding_amount ELSE 0 END) more_than_90,
                       SUM(outstanding_amount) total_outstanding
                FROM invoices WHERE status <> 'CANCELLED' AND outstanding_amount > 0
                """);
        common(sql, f, "party_id", "site_id", "agreement_id", null);
        date(sql, f, "invoice_date");
        sql.append(" GROUP BY party_legal_name_snapshot, site_name_snapshot ORDER BY total_outstanding DESC");
        return sql.spec();
    }

    private QuerySpec partySiteLedger(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT ledger_date date, document_number, entry_type, party, site, debit, credit,
                       SUM(debit-credit) OVER (ORDER BY ledger_date, sort_order, document_number) running_outstanding
                FROM (
                  SELECT invoice_date ledger_date, invoice_number document_number, 'INVOICE' entry_type, party_legal_name_snapshot party, site_name_snapshot site, grand_total debit, 0 credit, 1 sort_order, party_id, site_id, agreement_id FROM invoices WHERE status <> 'CANCELLED'
                  UNION ALL SELECT payment_date, receipt_number, 'CASH_PAYMENT', party_name_snapshot, site_name_snapshot, 0, cash_amount, 2, party_id, site_id, NULL FROM payment_receipts WHERE status='POSTED'
                  UNION ALL SELECT payment_date, receipt_number, 'TDS', party_name_snapshot, site_name_snapshot, 0, tds_amount, 3, party_id, site_id, NULL FROM payment_receipts WHERE status='POSTED'
                  UNION ALL SELECT transaction_date, deposit_number, 'DEPOSIT_ADJUSTMENT', party_name_snapshot, site_name_snapshot, 0, amount, 4, party_id, site_id, agreement_id FROM security_deposit_transactions WHERE status='POSTED' AND transaction_type='ADJUSTMENT_TO_INVOICE'
                ) x WHERE 1=1
                """);
        common(sql, f, "party_id", "site_id", "agreement_id", null);
        date(sql, f, "ledger_date");
        doc(sql, f, "document_number");
        sql.append(" ORDER BY ledger_date, sort_order, document_number");
        return sql.spec();
    }

    private QuerySpec gstSales(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT company_gstin_snapshot supplier_gstin, invoice_number, invoice_date, party_gstin_snapshot customer_gstin,
                       party_legal_name_snapshot customer_name, party_state_snapshot place_of_supply, grand_total invoice_value,
                       taxable_amount taxable_value, cgst_rate + sgst_rate + igst_rate tax_rate, cgst_amount cgst, sgst_amount sgst, igst_amount igst,
                       NULL hsn_sac, 'N' reverse_charge, CASE WHEN party_gstin_snapshot IS NULL OR party_gstin_snapshot='' THEN 'B2C' ELSE 'B2B' END invoice_type,
                       CASE WHEN company_gstin_snapshot REGEXP '^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$' AND (party_gstin_snapshot IS NULL OR party_gstin_snapshot='' OR party_gstin_snapshot REGEXP '^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$') THEN 'OK' ELSE 'MISSING_OR_INVALID_GSTIN' END validation_status
                FROM invoices WHERE status <> 'CANCELLED'
                """);
        common(sql, f, "party_id", "site_id", "agreement_id", null);
        date(sql, f, "invoice_date");
        doc(sql, f, "invoice_number");
        sql.append(" ORDER BY invoice_date, invoice_number");
        return sql.spec("GST preparation exports are review files only; they do not file a GST return.");
    }

    private QuerySpec gstTaxSummary(ReportFilterRequest f) {
        Sql sql = new Sql("""
                SELECT party_state_snapshot place_of_supply, cgst_rate + sgst_rate + igst_rate tax_rate,
                       SUM(taxable_amount) taxable_value, SUM(cgst_amount) cgst, SUM(sgst_amount) sgst, SUM(igst_amount) igst,
                       SUM(total_tax) total_tax, SUM(grand_total) invoice_value, COUNT(*) invoice_count
                FROM invoices WHERE status <> 'CANCELLED'
                """);
        common(sql, f, "party_id", "site_id", "agreement_id", null);
        date(sql, f, "invoice_date");
        sql.append(" GROUP BY party_state_snapshot, cgst_rate, sgst_rate, igst_rate ORDER BY party_state_snapshot, tax_rate");
        return sql.spec("GSTR-3B/GST summaries are preparation aids only and must be reviewed before filing.");
    }

    private long count(QuerySpec spec) {
        Number n = jdbc.queryForObject("SELECT COUNT(*) FROM (" + spec.sql() + ") report_count", spec.params(), Number.class);
        return n == null ? 0 : n.longValue();
    }

    private ReportFilterRequest normalize(ReportFilterRequest f) {
        if (f == null) return new ReportFilterRequest(null, null, null, null, null, null, null, null, null, null, null, 0, DEFAULT_SIZE);
        return f;
    }

    private ReportFilterRequest monthFilter(ReportFilterRequest f) {
        YearMonth month = f.month() != null ? f.month() : (f.startDate() != null ? YearMonth.from(f.startDate()) : YearMonth.now());
        return new ReportFilterRequest(month.atDay(1), month.atEndOfMonth(), f.partyId(), f.siteId(), f.agreementId(), f.itemId(), f.categoryId(), f.status(), f.documentNumber(), f.user(), month, f.page(), f.size());
    }

    private void common(Sql sql, ReportFilterRequest f, String partyCol, String siteCol, String agreementCol, String itemCol) {
        if (f.partyId() != null && partyCol != null) sql.eq(partyCol, "partyId", f.partyId());
        if (f.siteId() != null && siteCol != null) sql.eq(siteCol, "siteId", f.siteId());
        if (f.agreementId() != null && agreementCol != null) sql.eq(agreementCol, "agreementId", f.agreementId());
        if (f.itemId() != null && itemCol != null) sql.eq(itemCol, "itemId", f.itemId());
    }

    private void itemFilters(Sql sql, ReportFilterRequest f, String itemAlias, String categoryAlias) {
        if (f.itemId() != null) sql.eq(itemAlias + ".id", "itemId", f.itemId());
        if (f.categoryId() != null) sql.eq(categoryAlias + ".id", "categoryId", f.categoryId());
    }

    private void date(Sql sql, ReportFilterRequest f, String column) {
        if (f.startDate() != null) sql.gte(column, "startDate", f.startDate());
        if (f.endDate() != null) sql.lte(column, "endDate", f.endDate());
    }

    private void status(Sql sql, ReportFilterRequest f, String column) {
        if (has(f.status())) sql.eq(column, "status" + sql.params.getValues().size(), f.status().trim().toUpperCase(Locale.ROOT));
    }

    private void doc(Sql sql, ReportFilterRequest f, String column) {
        if (has(f.documentNumber())) sql.like(column, "documentNumber" + sql.params.getValues().size(), f.documentNumber().trim());
    }

    private Map<String, Object> ordered(Map<String, Object> row) {
        Map<String, Object> map = new LinkedHashMap<>();
        row.forEach((k, v) -> map.put(k.toLowerCase(Locale.ROOT), v));
        return map;
    }

    private Map<String, BigDecimal> totals(List<Map<String, Object>> rows) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            row.forEach((key, value) -> {
                if (value instanceof Number && !(value instanceof Integer) && !(value instanceof Long)) {
                    result.merge(key, bd(value), BigDecimal::add);
                }
            });
        }
        return result;
    }

    private BigDecimal bd(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }

    private boolean has(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static final class Sql {
        private final StringBuilder builder;
        private final MapSqlParameterSource params = new MapSqlParameterSource();
        Sql(String base) { this.builder = new StringBuilder(base); }
        void append(String sql) { builder.append(sql); }
        void eq(String column, String name, Object value) { builder.append(" AND ").append(column).append(" = :").append(name); params.addValue(name, value); }
        void gte(String column, String name, Object value) { builder.append(" AND ").append(column).append(" >= :").append(name); params.addValue(name, value); }
        void lte(String column, String name, Object value) { builder.append(" AND ").append(column).append(" <= :").append(name); params.addValue(name, value); }
        void like(String column, String name, String value) { builder.append(" AND ").append(column).append(" LIKE :").append(name); params.addValue(name, "%" + value + "%"); }
        QuerySpec spec() { return spec(null); }
        QuerySpec spec(String warning) { return new QuerySpec(builder.toString(), params, List.of(), warning); }
    }

    private record QuerySpec(String sql, MapSqlParameterSource params, List<String> columns, String warning) {}
}

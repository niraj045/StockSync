package com.stocksync.dashboard;

import com.stocksync.BaseIntegrationTest;
import com.stocksync.dashboard.service.DashboardOverviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Phase10DashboardIntegrationTest extends BaseIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired DashboardOverviewService dashboard;

    Long partyId;
    Long siteId;
    Long otherSiteId;
    Long agreementId;
    Long itemId;

    @BeforeEach
    void setup() {
        clean();
        jdbc.update("INSERT INTO parties(legal_name,gstin,address,state,active,created_by,updated_by) VALUES('Dashboard Party','24ABCDE1234F1Z5','Ahmedabad','GUJARAT',TRUE,'test','test')");
        partyId = id();
        jdbc.update("INSERT INTO sites(party_id,site_name,site_code,address,status,created_by,updated_by) VALUES(?,?,?,?,?,?,?)", partyId, "Dashboard Site", "DASH", "Ahmedabad", "ACTIVE", "test", "test");
        siteId = id();
        jdbc.update("INSERT INTO sites(party_id,site_name,site_code,address,status,created_by,updated_by) VALUES(?,?,?,?,?,?,?)", partyId, "Other Site", "OTH", "Ahmedabad", "ACTIVE", "test", "test");
        otherSiteId = id();
        jdbc.update("INSERT INTO item_categories(name,active,created_by,updated_by) VALUES('Dashboard Category',TRUE,'test','test')");
        Long categoryId = id();
        jdbc.update("INSERT INTO items(category_id,item_code,item_name,unit,minimum_stock,active,created_by,updated_by) VALUES(?,?,?,?,?,?,?,?)",
                categoryId, "DASH-ITEM", "Dashboard Item", "PCS", new BigDecimal("10.0000"), true, "test", "test");
        itemId = id();
        jdbc.update("""
                INSERT INTO agreements(agreement_number,party_id,site_id,agreement_date,effective_date,expiry_date,rental_type,billing_cycle,status,security_deposit,
                  party_legal_name_snapshot,party_address_snapshot,party_state_snapshot,site_name_snapshot,site_code_snapshot,site_address_snapshot,created_by,updated_by)
                VALUES('AGR-DASH',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, partyId, siteId, LocalDate.now().minusDays(20), LocalDate.now().minusDays(20), LocalDate.now().plusDays(12), "PER_PIECE_PER_DAY", "MONTHLY", "ACTIVE",
                new BigDecimal("500.00"), "Dashboard Party", "Ahmedabad", "GUJARAT", "Dashboard Site", "DASH", "Ahmedabad", "test", "test");
        agreementId = id();
        jdbc.update("INSERT INTO stock_balances(item_id,available_quantity,damaged_quantity,lost_quantity,scrapped_quantity) VALUES(?,?,?,?,?)",
                itemId, new BigDecimal("8.0000"), new BigDecimal("1.0000"), new BigDecimal("2.0000"), new BigDecimal("3.0000"));
        jdbc.update("INSERT INTO site_stock_balances(site_id,item_id,pending_quantity) VALUES(?,?,?)", siteId, itemId, new BigDecimal("4.0000"));
        jdbc.update("INSERT INTO site_stock_balances(site_id,item_id,pending_quantity) VALUES(?,?,?)", otherSiteId, itemId, new BigDecimal("9.0000"));
        jdbc.update("INSERT INTO stock_transactions(item_id,transaction_type,transaction_date,quantity,direction,source_type,source_id,site_id,party_id,created_by) VALUES(?,?,?,?,?,?,?,?,?,?)",
                itemId, "ISSUE", LocalDate.now(), new BigDecimal("4.0000"), "OUT", "ISSUED_CHALLAN", 1L, siteId, partyId, "test");
        jdbc.update("INSERT INTO stock_transactions(item_id,transaction_type,transaction_date,quantity,direction,source_type,source_id,site_id,party_id,created_by) VALUES(?,?,?,?,?,?,?,?,?,?)",
                itemId, "RETURN", LocalDate.now(), new BigDecimal("1.0000"), "IN", "RECEIVING_CHALLAN", 1L, siteId, partyId, "test");
        jdbc.update("INSERT INTO site_orders(order_number,agreement_id,party_id,site_id,order_date,status,created_by,updated_by) VALUES('SO-DASH',?,?,?,?,?,?,?)",
                agreementId, partyId, siteId, LocalDate.now(), "PARTIALLY_FULFILLED", "test", "test");
        invoice();
        jdbc.update("INSERT INTO loss_records(loss_number,source_type,agreement_id,party_id,site_id,item_id,loss_date,quantity,weight,charge_method,recovery_rate,calculated_recovery_amount,status,created_by,updated_by) VALUES('LR-DASH','MANUAL',?,?,?,?,?,?,?,?,?,?,?,?,?)",
                agreementId, partyId, siteId, itemId, LocalDate.now(), BigDecimal.ONE, BigDecimal.ZERO, "PER_PIECE", BigDecimal.TEN, BigDecimal.TEN, "PENDING_APPROVAL", "test", "test");
        jdbc.update("INSERT INTO damage_records(damage_number,source_type,agreement_id,party_id,site_id,item_id,damage_date,quantity,weight,repairable,damage_type,charge_method,damage_rate,calculated_damage_amount,status,created_by,updated_by) VALUES('DR-DASH','MANUAL',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                agreementId, partyId, siteId, itemId, LocalDate.now(), new BigDecimal("1.0000"), BigDecimal.ZERO, true, "BENT", "PER_PIECE", BigDecimal.TEN, BigDecimal.TEN, "UNDER_REPAIR", "test", "test");
    }

    @AfterEach
    void cleanup() {
        clean();
    }

    @Test
    void dashboardStockTotalsDoNotDoubleCountSiteMaterial() {
        var admin = auth("ROLE_ADMIN");
        var overview = dashboard.overview(partyId, null, null, LocalDate.now().minusDays(7), LocalDate.now(), admin);
        assertMoney("8.0000", overview.stockSummary().godownAvailable());
        assertMoney("13.0000", overview.stockSummary().materialAtSites());
        assertMoney("22.0000", overview.stockSummary().physicalCurrentStock());
        assertMoney("24.0000", overview.stockSummary().currentAccountableStock());
        assertEquals(1, overview.exceptionSummary().lossAwaitingApproval());
        assertEquals(1, overview.exceptionSummary().partiallyFulfilledOrders());
        assertTrue(overview.attentionItems().stream().anyMatch(i -> i.key().equals("low-stock")));
    }

    @Test
    void filtersAndRolesAreApplied() {
        var accounts = auth("ROLE_ACCOUNTS");
        var viewer = auth("ROLE_VIEWER");
        var filtered = dashboard.overview(partyId, siteId, null, LocalDate.now().minusDays(7), LocalDate.now(), accounts);
        assertMoney("4.0000", filtered.stockSummary().materialAtSites());
        assertMoney("1000.00", filtered.billingSummary().totalOutstanding());
        assertFalse(filtered.quickActions().stream().anyMatch(a -> a.key().equals("order")));
        assertTrue(filtered.quickActions().stream().anyMatch(a -> a.key().equals("payment")));

        var readOnly = dashboard.overview(partyId, siteId, null, LocalDate.now().minusDays(7), LocalDate.now(), viewer);
        assertEquals("READ_ONLY", readOnly.roleMode());
        assertEquals(0, readOnly.billingSummary().issuedInvoices());
        assertTrue(readOnly.quickActions().isEmpty());
    }

    private void invoice() {
        jdbc.update("INSERT INTO billing_runs(billing_run_number,agreement_id,party_id,site_id,period_start,period_end,status,grand_total,created_by,updated_by) VALUES('BR-DASH',?,?,?,?,?,?,?,?,?)",
                agreementId, partyId, siteId, LocalDate.now().minusDays(30), LocalDate.now(), "FINALIZED", new BigDecimal("1000.00"), "test", "test");
        Long runId = id();
        jdbc.update("""
                INSERT INTO invoices(invoice_number,billing_run_id,agreement_id,party_id,site_id,invoice_date,due_date,period_start,period_end,status,
                 company_name_snapshot,company_address_snapshot,company_gstin_snapshot,party_legal_name_snapshot,party_gstin_snapshot,party_address_snapshot,party_state_snapshot,
                 site_name_snapshot,site_code_snapshot,site_address_snapshot,agreement_number_snapshot,taxable_amount,cgst_rate,cgst_amount,sgst_rate,sgst_amount,igst_rate,igst_amount,total_tax,grand_total,cash_allocated_total,tds_allocated_total,outstanding_amount,created_by,updated_by)
                VALUES('INV-DASH',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, runId, agreementId, partyId, siteId, LocalDate.now(), LocalDate.now().minusDays(2), LocalDate.now().minusDays(30), LocalDate.now(), "ISSUED",
                "StockSync", "Ahmedabad", "24AAAAA1111A1Z1", "Dashboard Party", "24ABCDE1234F1Z5", "Ahmedabad", "GUJARAT", "Dashboard Site", "DASH", "Ahmedabad",
                "AGR-DASH", new BigDecimal("820.00"), new BigDecimal("9.0000"), new BigDecimal("90.00"), new BigDecimal("9.0000"), new BigDecimal("90.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("180.00"), new BigDecimal("1000.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1000.00"), "test", "test");
    }

    private UsernamePasswordAuthenticationToken auth(String role) {
        return new UsernamePasswordAuthenticationToken("user", "n/a", List.of(new SimpleGrantedAuthority(role)));
    }

    private Long id() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private void clean() {
        jdbc.update("SET FOREIGN_KEY_CHECKS = 0");
        for (String table : List.of(
                "report_export_history", "saved_report_filters", "deposit_invoice_allocations", "security_deposit_transactions", "tds_details",
                "payment_allocations", "payment_receipts", "invoice_items", "invoices", "billing_run_segments", "billing_run_charges",
                "billing_source_allocations", "billing_runs", "damage_records", "loss_records", "site_orders", "site_stock_balances",
                "stock_transactions", "agreement_item_slabs", "agreement_items", "agreements", "sites", "parties", "stock_balances", "items", "item_categories")) {
            jdbc.update("DELETE FROM " + table);
        }
        jdbc.update("SET FOREIGN_KEY_CHECKS = 1");
    }
}

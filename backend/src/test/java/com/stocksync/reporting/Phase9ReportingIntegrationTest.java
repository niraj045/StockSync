package com.stocksync.reporting;

import com.stocksync.BaseIntegrationTest;
import com.stocksync.reporting.dto.ReportDtos.ExportFormat;
import com.stocksync.reporting.dto.ReportDtos.ReportFilterRequest;
import com.stocksync.reporting.dto.ReportDtos.ReportPreviewResponse;
import com.stocksync.reporting.service.ReportExportService;
import com.stocksync.reporting.service.ReportQueryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.junit.jupiter.api.Assertions.*;

class Phase9ReportingIntegrationTest extends BaseIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired ReportQueryService reports;
    @Autowired ReportExportService exports;

    Long partyId;
    Long siteId;
    Long agreementId;
    Long itemId;

    @BeforeEach
    void setup() {
        clean();
        jdbc.update("INSERT INTO parties(legal_name,gstin,address,state,active,created_by,updated_by) VALUES('Report Party','24ABCDE1234F1Z5','Ahmedabad','GUJARAT',TRUE,'test','test')");
        partyId = id();
        jdbc.update("INSERT INTO sites(party_id,site_name,site_code,address,status,created_by,updated_by) VALUES(?,?,?,?,?,?,?)", partyId, "Report Site", "RPT", "Ahmedabad", "ACTIVE", "test", "test");
        siteId = id();
        jdbc.update("INSERT INTO item_categories(name,active,created_by,updated_by) VALUES('Report Category',TRUE,'test','test')");
        Long categoryId = id();
        jdbc.update("""
                INSERT INTO items(category_id,item_code,item_name,unit,minimum_stock,active,created_by,updated_by)
                VALUES(?,?,?,?,?,?,?,?)
                """, categoryId, "RPT-ITEM", "Report Item", "PCS", new BigDecimal("10.0000"), true, "test", "test");
        itemId = id();
        jdbc.update("""
                INSERT INTO agreements(agreement_number,party_id,site_id,agreement_date,effective_date,rental_type,billing_cycle,status,security_deposit,
                  party_legal_name_snapshot,party_address_snapshot,party_state_snapshot,site_name_snapshot,site_code_snapshot,site_address_snapshot,created_by,updated_by)
                VALUES('AGR-RPT',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, partyId, siteId, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1), "PER_PIECE_PER_DAY", "MONTHLY", "ACTIVE",
                new BigDecimal("500.00"), "Report Party", "Ahmedabad", "GUJARAT", "Report Site", "RPT", "Ahmedabad", "test", "test");
        agreementId = id();
        jdbc.update("INSERT INTO stock_balances(item_id,available_quantity,damaged_quantity,lost_quantity,scrapped_quantity) VALUES(?,?,?,?,?)",
                itemId, new BigDecimal("8.0000"), new BigDecimal("1.0000"), BigDecimal.ZERO, BigDecimal.ZERO);
        jdbc.update("INSERT INTO site_stock_balances(site_id,item_id,pending_quantity) VALUES(?,?,?)", siteId, itemId, new BigDecimal("4.0000"));
        jdbc.update("""
                INSERT INTO stock_transactions(item_id,transaction_type,transaction_date,quantity,direction,source_type,source_id,site_id,party_id,created_by)
                VALUES(?,?,?,?,?,?,?,?,?,?)
                """, itemId, "ISSUE", LocalDate.of(2026, 7, 5), new BigDecimal("4.0000"), "OUT", "ISSUED_CHALLAN", 1L, siteId, partyId, "test");
        Long invoice1 = invoice("INV-RPT-1", "BR-RPT-1", LocalDate.now().minusDays(10), new BigDecimal("1000.00"), new BigDecimal("300.00"), "ISSUED", "24ABCDE1234F1Z5");
        invoice("INV-RPT-CANCELLED", "BR-RPT-C", LocalDate.now().minusDays(95), new BigDecimal("999.00"), new BigDecimal("999.00"), "CANCELLED", "BADGSTIN");
        jdbc.update("""
                INSERT INTO payment_receipts(receipt_number,party_id,site_id,party_name_snapshot,site_name_snapshot,payment_date,payment_mode,cash_amount,tds_amount,total_settlement_amount,unallocated_amount,status,created_by,updated_by)
                VALUES('PR-RPT-1',?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, partyId, siteId, "Report Party", "Report Site", LocalDate.of(2026, 7, 15), "NEFT", new BigDecimal("600.00"),
                new BigDecimal("100.00"), new BigDecimal("700.00"), BigDecimal.ZERO, "POSTED", "test", "test");
        Long paymentId = id();
        jdbc.update("INSERT INTO payment_allocations(payment_receipt_id,invoice_id,cash_allocated,tds_allocated,total_allocated,created_by,updated_by) VALUES(?,?,?,?,?,?,?)",
                paymentId, invoice1, new BigDecimal("600.00"), new BigDecimal("100.00"), new BigDecimal("700.00"), "test", "test");
        jdbc.update("INSERT INTO tds_details(payment_receipt_id,tds_amount,section,certificate_number,verification_status,created_by,updated_by) VALUES(?,?,?,?,?,?,?)",
                paymentId, new BigDecimal("100.00"), "194C", "CERT-RPT", "VERIFIED", "test", "test");
        jdbc.update("""
                INSERT INTO security_deposit_transactions(deposit_number,agreement_id,party_id,site_id,agreement_number_snapshot,party_name_snapshot,site_name_snapshot,transaction_type,transaction_date,amount,status,created_by,updated_by)
                VALUES('SD-RPT-1',?,?,?,?,?,?,?,?,?,?,?,?)
                """, agreementId, partyId, siteId, "AGR-RPT", "Report Party", "Report Site", "RECEIPT", LocalDate.of(2026, 7, 2), new BigDecimal("500.00"), "POSTED", "test", "test");
    }

    @AfterEach
    void cleanup() {
        clean();
    }

    @Test
    void inventoryOutstandingPaymentAndGstReportsUseSourceTotals() {
        ReportFilterRequest filter = new ReportFilterRequest(null, null, partyId, siteId, agreementId, itemId, null, null, null, null, null, 0, 25);

        ReportPreviewResponse stock = reports.preview("CURRENT_STOCK_SUMMARY", filter);
        assertEquals(1, stock.rows().size());
        assertMoney("8.0000", stock.rows().getFirst().get("godown_available"));
        assertMoney("4.0000", stock.rows().getFirst().get("at_sites"));
        assertMoney("13.0000", stock.rows().getFirst().get("current_accountable_stock"));

        ReportPreviewResponse ageing = reports.preview("OUTSTANDING_AGEING", filter);
        assertEquals(1, ageing.rows().size());
        assertMoney("300.00", ageing.totals().get("days_1_to_30"));
        assertMoney("300.00", ageing.totals().get("total_outstanding"));

        ReportPreviewResponse payments = reports.preview("PAYMENT_REGISTER", filter);
        assertMoney("600.00", payments.totals().get("cash_amount"));
        assertMoney("100.00", payments.totals().get("tds_amount"));

        ReportPreviewResponse gst = reports.preview("GST_SALES_REGISTER", filter);
        assertEquals("OK", gst.rows().getFirst().get("validation_status"));
        assertTrue(gst.warning().contains("do not file"));
    }

    @Test
    void exportsAreGeneratedAndSensitiveReportsAreRoleProtected() {
        var accounts = new UsernamePasswordAuthenticationToken("accounts", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ACCOUNTS")));
        var viewer = new UsernamePasswordAuthenticationToken("viewer", "n/a", List.of(new SimpleGrantedAuthority("ROLE_VIEWER")));
        ReportFilterRequest filter = new ReportFilterRequest(null, null, partyId, siteId, agreementId, null, null, null, null, null, null, 0, 25);

        assertDoesNotThrow(() -> exports.export("GST_SALES_REGISTER", filter, ExportFormat.EXCEL, accounts, null));
        assertDoesNotThrow(() -> exports.export("MONTHLY_SITE_STATEMENT", filter, ExportFormat.PDF, accounts, null));
        assertThrows(AccessDeniedException.class, () -> exports.export("PAYMENT_REGISTER", filter, ExportFormat.CSV, viewer, null));
    }

    private Long invoice(String invoiceNumber, String runNumber, LocalDate dueDate, BigDecimal total, BigDecimal outstanding, String status, String partyGstin) {
        jdbc.update("""
                INSERT INTO billing_runs(billing_run_number,agreement_id,party_id,site_id,period_start,period_end,status,grand_total,created_by,updated_by)
                VALUES(?,?,?,?,?,?,?,?,?,?)
                """, runNumber, agreementId, partyId, siteId, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "FINALIZED", total, "test", "test");
        Long runId = id();
        jdbc.update("""
                INSERT INTO invoices(invoice_number,billing_run_id,agreement_id,party_id,site_id,invoice_date,due_date,period_start,period_end,status,
                 company_name_snapshot,company_address_snapshot,company_gstin_snapshot,party_legal_name_snapshot,party_gstin_snapshot,party_address_snapshot,party_state_snapshot,
                 site_name_snapshot,site_code_snapshot,site_address_snapshot,agreement_number_snapshot,taxable_amount,cgst_rate,cgst_amount,sgst_rate,sgst_amount,igst_rate,igst_amount,total_tax,grand_total,cash_allocated_total,tds_allocated_total,outstanding_amount,created_by,updated_by)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, invoiceNumber, runId, agreementId, partyId, siteId, LocalDate.of(2026, 7, 31), dueDate, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), status,
                "StockSync", "Ahmedabad", "24AAAAA1111A1Z1", "Report Party", partyGstin, "Ahmedabad", "GUJARAT", "Report Site", "RPT", "Ahmedabad",
                "AGR-RPT", total.subtract(new BigDecimal("180.00")), new BigDecimal("9.0000"), new BigDecimal("90.00"), new BigDecimal("9.0000"), new BigDecimal("90.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("180.00"), total, total.subtract(outstanding).subtract(new BigDecimal("100.00")).max(BigDecimal.ZERO), new BigDecimal("100.00"), outstanding, "test", "test");
        return id();
    }

    private Long id() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void assertMoney(String expected, Object actual) {
        assertEquals(0, new BigDecimal(expected).compareTo((BigDecimal) actual));
    }

    private void clean() {
        jdbc.update("SET FOREIGN_KEY_CHECKS = 0");
        for (String table : List.of(
                "report_export_history", "saved_report_filters", "deposit_invoice_allocations", "security_deposit_transactions", "tds_details",
                "payment_allocations", "payment_receipts", "invoice_items", "invoices", "billing_run_segments", "billing_run_charges",
                "billing_source_allocations", "billing_runs", "site_stock_balances", "stock_transactions", "agreement_item_slabs",
                "agreement_items", "agreements", "sites", "parties", "stock_balances", "items", "item_categories")) {
            jdbc.update("DELETE FROM " + table);
        }
        jdbc.update("SET FOREIGN_KEY_CHECKS = 1");
    }
}

package com.stocksync.reporting.service;

import com.stocksync.reporting.dto.ReportDtos.ReportDefinitionResponse;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class ReportCatalogService {
    private static final List<String> TABULAR = List.of("PDF", "EXCEL", "CSV");
    private static final List<String> STATEMENT = List.of("PDF", "EXCEL");
    private static final List<String> GST = List.of("EXCEL", "CSV");

    private final List<ReportDefinitionResponse> reports = List.of(
            def("CURRENT_STOCK_SUMMARY", "Current Stock Summary", "Inventory", TABULAR, opsAccountsViewer(), false, "Godown, site and accountable stock by item."),
            def("GODOWN_STOCK", "Godown Stock Report", "Inventory", TABULAR, opsAccountsViewer(), false, "Godown stock movement and closing balance."),
            def("SITE_PENDING_STOCK", "Site Pending Stock", "Inventory", TABULAR, opsAccountsViewer(), false, "Pending material at sites by party, agreement and item."),
            def("ITEM_LEDGER", "Item Ledger", "Inventory", TABULAR, opsAccountsViewer(), false, "Item transaction ledger with running movement columns."),
            def("LOW_STOCK", "Low Stock", "Inventory", TABULAR, opsAccountsViewer(), false, "Items below configured minimum stock."),
            def("DAMAGE_LOSS_SCRAP", "Damage, Loss and Scrap", "Inventory", TABULAR, opsAccountsViewer(), false, "Loss, damage and scrap records with charge values."),
            def("PARTY_STOCK_SUMMARY", "Party-wise Stock Summary", "Sites and Parties", TABULAR, opsAccountsViewer(), false, "Pending site stock grouped by party."),
            def("SITE_STOCK_SUMMARY", "Site-wise Stock Summary", "Sites and Parties", TABULAR, opsAccountsViewer(), false, "Pending stock grouped by site."),
            def("SITE_AGREEMENT_SUMMARY", "Site Agreement Summary", "Sites and Parties", TABULAR, opsAccountsViewer(), false, "Agreement status and deposit summary by site."),
            def("PARTY_OUTSTANDING", "Party Outstanding", "Sites and Parties", TABULAR, accountsViewer(), false, "Outstanding balance grouped by party."),
            def("SITE_OUTSTANDING", "Site Outstanding", "Sites and Parties", TABULAR, accountsViewer(), false, "Outstanding balance grouped by site."),
            def("MONTHLY_SITE_STATEMENT", "Monthly Site Statement", "Sites and Parties", STATEMENT, accountsOps(), false, "Monthly material and financial statement for one site."),
            def("SITE_ORDERS_REGISTER", "Site Orders Register", "Operations", TABULAR, opsViewer(), false, "Site order register with fulfilment quantities."),
            def("ISSUED_CHALLANS_REGISTER", "Issued Challans Register", "Operations", TABULAR, opsViewer(), false, "Issued challan movement register."),
            def("RECEIVING_CHALLANS_REGISTER", "Receiving Challans Register", "Operations", TABULAR, opsViewer(), false, "Receiving challan register with return/loss/damage quantities."),
            def("SITE_TRANSFERS_REGISTER", "Site Transfers Register", "Operations", TABULAR, opsViewer(), false, "Site transfer register."),
            def("LOSS_RECORDS_REGISTER", "Loss Records Register", "Operations", TABULAR, opsViewer(), false, "Loss record register."),
            def("DAMAGE_RECORDS_REGISTER", "Damage Records Register", "Operations", TABULAR, opsViewer(), false, "Damage record register."),
            def("ITEM_EXCHANGES_REGISTER", "Item Exchanges Register", "Operations", TABULAR, opsViewer(), false, "Item exchange register."),
            def("PURCHASES_REGISTER", "Purchases Register", "Operations", TABULAR, opsViewer(), false, "Purchase register."),
            def("SCRAP_REGISTER", "Scrap Register", "Operations", TABULAR, opsViewer(), false, "Scrap register."),
            def("STOCK_ADJUSTMENTS_REGISTER", "Stock Adjustments Register", "Operations", TABULAR, opsViewer(), false, "Stock adjustment register."),
            def("QUOTATION_REGISTER", "Quotation Register", "Commercial", TABULAR, commercialRoles(), false, "Quotation commercial register."),
            def("AGREEMENT_REGISTER", "Agreement Register", "Commercial", TABULAR, commercialRoles(), false, "Agreement commercial register."),
            def("BILLING_RUN_REGISTER", "Billing Run Register", "Commercial", TABULAR, accountsViewer(), false, "Billing-run register."),
            def("INVOICE_REGISTER", "Invoice Register", "Financial", TABULAR, accountsViewer(), false, "Invoice totals, tax and outstanding."),
            def("PAYMENT_REGISTER", "Payment Register", "Financial", TABULAR, accountsOnly(), false, "Payment receipt register."),
            def("TDS_REPORT", "TDS Report", "Financial", TABULAR, accountsOnly(), false, "TDS deductions and verification status."),
            def("SECURITY_DEPOSIT_REPORT", "Security Deposit Report", "Financial", TABULAR, accountsOnly(), false, "Required, received, adjusted, refunded and available deposits."),
            def("OUTSTANDING_AGEING", "Outstanding Ageing", "Financial", TABULAR, accountsViewer(), false, "Due-date based ageing buckets."),
            def("PARTY_SITE_LEDGER", "Party and Site Ledger", "Financial", TABULAR, accountsViewer(), false, "Invoices, cash, TDS and deposit adjustments with running balance."),
            def("GST_SALES_REGISTER", "GST Sales Register", "GST", GST, accountsOnly(), true, "GST outward supply preparation register."),
            def("GST_TAX_SUMMARY", "GST Tax Summary", "GST", GST, accountsOnly(), true, "Tax-rate, CGST, SGST and IGST summary."),
            def("GSTR1_PREPARATION", "GSTR-1 Preparation Export", "GST", GST, accountsOnly(), true, "Preparation export for accountant review, not filing."),
            def("GSTR3B_SUMMARY", "GSTR-3B Summary Preparation", "GST", GST, accountsOnly(), true, "Outward liability summary for accountant review, not filing.")
    );

    public List<ReportDefinitionResponse> visibleReports(Authentication authentication) {
        Set<String> roles = roles(authentication);
        return reports.stream().filter(report -> canAccess(report.reportType(), roles)).toList();
    }

    public ReportDefinitionResponse require(String reportType, Authentication authentication) {
        String type = normalize(reportType);
        ReportDefinitionResponse report = reports.stream()
                .filter(r -> r.reportType().equals(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported report type"));
        if (!canAccess(type, roles(authentication))) {
            throw new org.springframework.security.access.AccessDeniedException("Report not permitted");
        }
        return report;
    }

    public boolean canAccess(String reportType, Set<String> roles) {
        if (roles.contains("ROLE_ADMIN")) return true;
        ReportDefinitionResponse report = reports.stream().filter(r -> r.reportType().equals(normalize(reportType))).findFirst().orElse(null);
        if (report == null) return false;
        return report.roles().stream().anyMatch(roles::contains);
    }

    public String normalize(String reportType) {
        return reportType == null ? "" : reportType.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }

    private Set<String> roles(Authentication authentication) {
        if (authentication == null) return Set.of();
        return authentication.getAuthorities().stream().map(a -> a.getAuthority()).collect(java.util.stream.Collectors.toSet());
    }

    private static ReportDefinitionResponse def(String type, String name, String category, List<String> formats, List<String> roles, boolean gst, String description) {
        return new ReportDefinitionResponse(type, name, category, formats, roles, gst, description);
    }
    private static List<String> opsAccountsViewer() { return List.of("ROLE_OPERATIONS", "ROLE_ACCOUNTS", "ROLE_VIEWER"); }
    private static List<String> opsViewer() { return List.of("ROLE_OPERATIONS", "ROLE_VIEWER"); }
    private static List<String> accountsViewer() { return List.of("ROLE_ACCOUNTS", "ROLE_VIEWER"); }
    private static List<String> accountsOnly() { return List.of("ROLE_ACCOUNTS"); }
    private static List<String> accountsOps() { return List.of("ROLE_ACCOUNTS", "ROLE_OPERATIONS", "ROLE_VIEWER"); }
    private static List<String> commercialRoles() { return List.of("ROLE_ACCOUNTS", "ROLE_OPERATIONS", "ROLE_VIEWER"); }
}

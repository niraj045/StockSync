package com.stocksync.payment.service;

import com.stocksync.agreement.entity.Agreement;
import com.stocksync.agreement.repository.AgreementRepository;
import com.stocksync.billing.entity.Invoice;
import com.stocksync.billing.repository.InvoiceRepository;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.payment.dto.OutstandingDtos.*;
import com.stocksync.payment.repository.SecurityDepositTransactionRepository;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutstandingService {
    private final InvoiceRepository invoices;
    private final AgreementRepository agreements;
    private final SecurityDepositTransactionRepository deposits;
    private final JdbcTemplate jdbc;
    public OutstandingService(InvoiceRepository invoices, AgreementRepository agreements, SecurityDepositTransactionRepository deposits, JdbcTemplate jdbc) {
        this.invoices = invoices; this.agreements = agreements; this.deposits = deposits; this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public InvoiceOutstandingResponse invoice(Long id) {
        Invoice i = invoices.findById(id).orElseThrow(() -> new BusinessRuleException("INVOICE_NOT_FOUND", "Invoice not found"));
        return new InvoiceOutstandingResponse(i.getId(), i.getInvoiceNumber(), i.getGrandTotal(), i.getCashAllocatedTotal(), i.getTdsAllocatedTotal(), i.getDepositAdjustedTotal(), i.getOutstandingAmount(), paymentStatus(i));
    }

    @Transactional(readOnly = true)
    public SummaryResponse site(Long siteId) { return summary("site_id", siteId); }

    @Transactional(readOnly = true)
    public SummaryResponse party(Long partyId) { return summary("party_id", partyId); }

    @Transactional(readOnly = true)
    public AgreementOutstandingResponse agreement(Long agreementId) {
        Agreement a = agreements.findById(agreementId).orElseThrow(() -> new BusinessRuleException("AGREEMENT_NOT_FOUND", "Agreement not found"));
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT COALESCE(SUM(grand_total),0) total_billed,
                       COALESCE(SUM(cash_allocated_total + tds_allocated_total + deposit_adjusted_total),0) total_settled,
                       COALESCE(SUM(outstanding_amount),0) outstanding
                FROM invoices WHERE status <> 'CANCELLED' AND agreement_id = ?
                """, agreementId);
        return new AgreementOutstandingResponse(bd(row.get("total_billed")), bd(row.get("total_settled")), bd(row.get("outstanding")), a.getSecurityDeposit(), deposits.availableForAgreement(agreementId));
    }

    private SummaryResponse summary(String column, Long id) {
        Map<String, Object> invoice = jdbc.queryForMap("""
                SELECT COALESCE(SUM(grand_total),0) total_billed,
                       COALESCE(SUM(cash_allocated_total),0) cash_received,
                       COALESCE(SUM(tds_allocated_total),0) tds,
                       COALESCE(SUM(deposit_adjusted_total),0) deposit_adjustments,
                       COALESCE(SUM(outstanding_amount),0) outstanding
                FROM invoices WHERE status <> 'CANCELLED' AND %s = ?
                """.formatted(column), id);
        BigDecimal advance = bd(jdbc.queryForObject("""
                SELECT COALESCE(SUM(unallocated_amount),0) FROM payment_receipts
                WHERE status = 'POSTED' AND %s = ?
                """.formatted(column.equals("site_id") ? "site_id" : "party_id"), BigDecimal.class, id));
        BigDecimal deposit = bd(jdbc.queryForObject("""
                SELECT COALESCE(SUM(CASE WHEN status = 'POSTED' AND transaction_type = 'RECEIPT' THEN amount WHEN status = 'POSTED' AND transaction_type IN ('REFUND','ADJUSTMENT_TO_INVOICE') THEN -amount ELSE 0 END),0)
                FROM security_deposit_transactions WHERE %s = ?
                """.formatted(column), BigDecimal.class, id));
        return new SummaryResponse(bd(invoice.get("total_billed")), bd(invoice.get("cash_received")), bd(invoice.get("tds")), bd(invoice.get("deposit_adjustments")), bd(invoice.get("outstanding")), advance, deposit);
    }

    private String paymentStatus(Invoice i) {
        if (i.getOutstandingAmount().signum() <= 0) return "PAID";
        if (i.getOutstandingAmount().compareTo(i.getGrandTotal()) < 0) return "PARTIALLY_PAID";
        return "UNPAID";
    }
    private BigDecimal bd(Object value) { return value == null ? BigDecimal.ZERO : (BigDecimal) value; }
}

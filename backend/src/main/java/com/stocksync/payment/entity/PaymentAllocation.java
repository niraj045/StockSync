package com.stocksync.payment.entity;

import com.stocksync.billing.entity.Invoice;
import com.stocksync.common.persistence.AuditedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "payment_allocations", uniqueConstraints = @UniqueConstraint(name = "uk_payment_allocation_invoice", columnNames = {"payment_receipt_id", "invoice_id"}))
public class PaymentAllocation extends AuditedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "payment_receipt_id") private PaymentReceipt paymentReceipt;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "invoice_id") private Invoice invoice;
    @Column(name = "cash_allocated", nullable = false, precision = 19, scale = 2) private BigDecimal cashAllocated = BigDecimal.ZERO;
    @Column(name = "tds_allocated", nullable = false, precision = 19, scale = 2) private BigDecimal tdsAllocated = BigDecimal.ZERO;
    @Column(name = "total_allocated", nullable = false, precision = 19, scale = 2) private BigDecimal totalAllocated = BigDecimal.ZERO;
    public Long getId(){return id;} public PaymentReceipt getPaymentReceipt(){return paymentReceipt;} public void setPaymentReceipt(PaymentReceipt v){paymentReceipt=v;}
    public Invoice getInvoice(){return invoice;} public void setInvoice(Invoice v){invoice=v;} public BigDecimal getCashAllocated(){return cashAllocated;} public void setCashAllocated(BigDecimal v){cashAllocated=v;}
    public BigDecimal getTdsAllocated(){return tdsAllocated;} public void setTdsAllocated(BigDecimal v){tdsAllocated=v;} public BigDecimal getTotalAllocated(){return totalAllocated;} public void setTotalAllocated(BigDecimal v){totalAllocated=v;}
}

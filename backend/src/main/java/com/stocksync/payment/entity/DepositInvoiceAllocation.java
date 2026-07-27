package com.stocksync.payment.entity;

import com.stocksync.billing.entity.Invoice;
import com.stocksync.common.persistence.AuditedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "deposit_invoice_allocations")
public class DepositInvoiceAllocation extends AuditedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "deposit_transaction_id") private SecurityDepositTransaction depositTransaction;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "invoice_id") private Invoice invoice;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount = BigDecimal.ZERO;
    public Long getId(){return id;} public SecurityDepositTransaction getDepositTransaction(){return depositTransaction;} public void setDepositTransaction(SecurityDepositTransaction v){depositTransaction=v;}
    public Invoice getInvoice(){return invoice;} public void setInvoice(Invoice v){invoice=v;} public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
}

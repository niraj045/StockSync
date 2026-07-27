package com.stocksync.payment.entity;

import com.stocksync.agreement.entity.Agreement;
import com.stocksync.billing.entity.Invoice;
import com.stocksync.common.persistence.AuditedEntity;
import com.stocksync.file.entity.FileAttachment;
import com.stocksync.party.entity.Party;
import com.stocksync.site.entity.Site;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "security_deposit_transactions")
public class SecurityDepositTransaction extends AuditedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "deposit_number", nullable = false, unique = true, length = 50) private String depositNumber;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "agreement_id") private Agreement agreement;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "party_id") private Party party;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "site_id") private Site site;
    @Column(name = "agreement_number_snapshot", nullable = false, length = 50) private String agreementNumberSnapshot;
    @Column(name = "party_name_snapshot", nullable = false) private String partyNameSnapshot;
    @Column(name = "site_name_snapshot", nullable = false) private String siteNameSnapshot;
    @Enumerated(EnumType.STRING) @Column(name = "transaction_type", nullable = false, length = 30) private SecurityDepositTransactionType transactionType;
    @Column(name = "transaction_date", nullable = false) private LocalDate transactionDate;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) @Column(name = "payment_mode", length = 30) private PaymentMode paymentMode;
    @Column(name = "reference_number", length = 100) private String referenceNumber;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "related_invoice_id") private Invoice relatedInvoice;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "source_deposit_transaction_id") private SecurityDepositTransaction sourceDepositTransaction;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private SecurityDepositStatus status = SecurityDepositStatus.POSTED;
    @Column(length = 1000) private String notes;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "attachment_id") private FileAttachment attachment;
    @Column(name = "posted_at") private Instant postedAt;
    @Column(name = "posted_by", length = 50) private String postedBy;
    @Column(name = "reversed_at") private Instant reversedAt;
    @Column(name = "reversed_by", length = 50) private String reversedBy;
    @Column(name = "reversal_reason", length = 500) private String reversalReason;
    public Long getId(){return id;} public String getDepositNumber(){return depositNumber;} public void setDepositNumber(String v){depositNumber=v;}
    public Agreement getAgreement(){return agreement;} public void setAgreement(Agreement v){agreement=v;} public Party getParty(){return party;} public void setParty(Party v){party=v;} public Site getSite(){return site;} public void setSite(Site v){site=v;}
    public String getAgreementNumberSnapshot(){return agreementNumberSnapshot;} public void setAgreementNumberSnapshot(String v){agreementNumberSnapshot=v;} public String getPartyNameSnapshot(){return partyNameSnapshot;} public void setPartyNameSnapshot(String v){partyNameSnapshot=v;} public String getSiteNameSnapshot(){return siteNameSnapshot;} public void setSiteNameSnapshot(String v){siteNameSnapshot=v;}
    public SecurityDepositTransactionType getTransactionType(){return transactionType;} public void setTransactionType(SecurityDepositTransactionType v){transactionType=v;} public LocalDate getTransactionDate(){return transactionDate;} public void setTransactionDate(LocalDate v){transactionDate=v;}
    public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;} public PaymentMode getPaymentMode(){return paymentMode;} public void setPaymentMode(PaymentMode v){paymentMode=v;}
    public String getReferenceNumber(){return referenceNumber;} public void setReferenceNumber(String v){referenceNumber=v;} public Invoice getRelatedInvoice(){return relatedInvoice;} public void setRelatedInvoice(Invoice v){relatedInvoice=v;}
    public SecurityDepositTransaction getSourceDepositTransaction(){return sourceDepositTransaction;} public void setSourceDepositTransaction(SecurityDepositTransaction v){sourceDepositTransaction=v;} public SecurityDepositStatus getStatus(){return status;} public void setStatus(SecurityDepositStatus v){status=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;} public FileAttachment getAttachment(){return attachment;} public void setAttachment(FileAttachment v){attachment=v;}
    public Instant getPostedAt(){return postedAt;} public void setPostedAt(Instant v){postedAt=v;} public String getPostedBy(){return postedBy;} public void setPostedBy(String v){postedBy=v;}
    public Instant getReversedAt(){return reversedAt;} public void setReversedAt(Instant v){reversedAt=v;} public String getReversedBy(){return reversedBy;} public void setReversedBy(String v){reversedBy=v;} public String getReversalReason(){return reversalReason;} public void setReversalReason(String v){reversalReason=v;}
}

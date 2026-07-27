package com.stocksync.payment.entity;

import com.stocksync.common.persistence.AuditedEntity;
import com.stocksync.file.entity.FileAttachment;
import com.stocksync.party.entity.Party;
import com.stocksync.site.entity.Site;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payment_receipts")
public class PaymentReceipt extends AuditedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "receipt_number", nullable = false, unique = true, length = 50) private String receiptNumber;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "party_id") private Party party;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "site_id") private Site site;
    @Column(name = "party_name_snapshot", nullable = false) private String partyNameSnapshot;
    @Column(name = "site_name_snapshot") private String siteNameSnapshot;
    @Column(name = "payment_date", nullable = false) private LocalDate paymentDate;
    @Enumerated(EnumType.STRING) @Column(name = "payment_mode", nullable = false, length = 30) private PaymentMode paymentMode;
    @Column(name = "reference_number", length = 100) private String referenceNumber;
    @Column(name = "bank_name", length = 150) private String bankName;
    @Column(name = "cheque_number", length = 100) private String chequeNumber;
    @Column(name = "cheque_date") private LocalDate chequeDate;
    @Column(name = "cash_amount", nullable = false, precision = 19, scale = 2) private BigDecimal cashAmount = BigDecimal.ZERO;
    @Column(name = "tds_amount", nullable = false, precision = 19, scale = 2) private BigDecimal tdsAmount = BigDecimal.ZERO;
    @Column(name = "total_settlement_amount", nullable = false, precision = 19, scale = 2) private BigDecimal totalSettlementAmount = BigDecimal.ZERO;
    @Column(name = "unallocated_amount", nullable = false, precision = 19, scale = 2) private BigDecimal unallocatedAmount = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentStatus status = PaymentStatus.DRAFT;
    @Column(length = 1000) private String notes;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "attachment_id") private FileAttachment attachment;
    @Column(name = "posted_at") private Instant postedAt;
    @Column(name = "posted_by", length = 50) private String postedBy;
    @Column(name = "reversed_at") private Instant reversedAt;
    @Column(name = "reversed_by", length = 50) private String reversedBy;
    @Column(name = "reversal_reason", length = 500) private String reversalReason;
    @OneToMany(mappedBy = "paymentReceipt", cascade = CascadeType.ALL, orphanRemoval = true) private List<PaymentAllocation> allocations = new ArrayList<>();
    @OneToOne(mappedBy = "paymentReceipt", cascade = CascadeType.ALL, orphanRemoval = true) private TdsDetails tdsDetails;
    public Long getId(){return id;} public String getReceiptNumber(){return receiptNumber;} public void setReceiptNumber(String v){receiptNumber=v;}
    public Party getParty(){return party;} public void setParty(Party v){party=v;} public Site getSite(){return site;} public void setSite(Site v){site=v;}
    public String getPartyNameSnapshot(){return partyNameSnapshot;} public void setPartyNameSnapshot(String v){partyNameSnapshot=v;} public String getSiteNameSnapshot(){return siteNameSnapshot;} public void setSiteNameSnapshot(String v){siteNameSnapshot=v;}
    public LocalDate getPaymentDate(){return paymentDate;} public void setPaymentDate(LocalDate v){paymentDate=v;} public PaymentMode getPaymentMode(){return paymentMode;} public void setPaymentMode(PaymentMode v){paymentMode=v;}
    public String getReferenceNumber(){return referenceNumber;} public void setReferenceNumber(String v){referenceNumber=v;} public String getBankName(){return bankName;} public void setBankName(String v){bankName=v;}
    public String getChequeNumber(){return chequeNumber;} public void setChequeNumber(String v){chequeNumber=v;} public LocalDate getChequeDate(){return chequeDate;} public void setChequeDate(LocalDate v){chequeDate=v;}
    public BigDecimal getCashAmount(){return cashAmount;} public void setCashAmount(BigDecimal v){cashAmount=v;} public BigDecimal getTdsAmount(){return tdsAmount;} public void setTdsAmount(BigDecimal v){tdsAmount=v;}
    public BigDecimal getTotalSettlementAmount(){return totalSettlementAmount;} public void setTotalSettlementAmount(BigDecimal v){totalSettlementAmount=v;} public BigDecimal getUnallocatedAmount(){return unallocatedAmount;} public void setUnallocatedAmount(BigDecimal v){unallocatedAmount=v;}
    public PaymentStatus getStatus(){return status;} public void setStatus(PaymentStatus v){status=v;} public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public FileAttachment getAttachment(){return attachment;} public void setAttachment(FileAttachment v){attachment=v;} public Instant getPostedAt(){return postedAt;} public void setPostedAt(Instant v){postedAt=v;}
    public String getPostedBy(){return postedBy;} public void setPostedBy(String v){postedBy=v;} public Instant getReversedAt(){return reversedAt;} public void setReversedAt(Instant v){reversedAt=v;}
    public String getReversedBy(){return reversedBy;} public void setReversedBy(String v){reversedBy=v;} public String getReversalReason(){return reversalReason;} public void setReversalReason(String v){reversalReason=v;}
    public List<PaymentAllocation> getAllocations(){return allocations;} public TdsDetails getTdsDetails(){return tdsDetails;} public void setTdsDetails(TdsDetails v){tdsDetails=v;if(v!=null)v.setPaymentReceipt(this);}
    public void addAllocation(PaymentAllocation allocation){allocation.setPaymentReceipt(this);allocations.add(allocation);}
}

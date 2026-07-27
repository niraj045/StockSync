package com.stocksync.payment.entity;

import com.stocksync.common.persistence.AuditedEntity;
import com.stocksync.file.entity.FileAttachment;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "tds_details")
public class TdsDetails extends AuditedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "payment_receipt_id") private PaymentReceipt paymentReceipt;
    @Column(name = "tds_amount", nullable = false, precision = 19, scale = 2) private BigDecimal tdsAmount = BigDecimal.ZERO;
    @Column(name = "deduction_date") private LocalDate deductionDate;
    @Column(length = 50) private String section;
    @Column(name = "certificate_number", length = 100) private String certificateNumber;
    @Column(name = "certificate_date") private LocalDate certificateDate;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "certificate_attachment_id") private FileAttachment certificateAttachment;
    @Enumerated(EnumType.STRING) @Column(name = "verification_status", nullable = false, length = 20) private TdsVerificationStatus verificationStatus = TdsVerificationStatus.PENDING;
    @Column(name = "rejection_reason", length = 500) private String rejectionReason;
    @Column(name = "verified_at") private Instant verifiedAt;
    @Column(name = "verified_by", length = 50) private String verifiedBy;
    public Long getId(){return id;} public PaymentReceipt getPaymentReceipt(){return paymentReceipt;} public void setPaymentReceipt(PaymentReceipt v){paymentReceipt=v;}
    public BigDecimal getTdsAmount(){return tdsAmount;} public void setTdsAmount(BigDecimal v){tdsAmount=v;} public LocalDate getDeductionDate(){return deductionDate;} public void setDeductionDate(LocalDate v){deductionDate=v;}
    public String getSection(){return section;} public void setSection(String v){section=v;} public String getCertificateNumber(){return certificateNumber;} public void setCertificateNumber(String v){certificateNumber=v;}
    public LocalDate getCertificateDate(){return certificateDate;} public void setCertificateDate(LocalDate v){certificateDate=v;} public FileAttachment getCertificateAttachment(){return certificateAttachment;} public void setCertificateAttachment(FileAttachment v){certificateAttachment=v;}
    public TdsVerificationStatus getVerificationStatus(){return verificationStatus;} public void setVerificationStatus(TdsVerificationStatus v){verificationStatus=v;} public String getRejectionReason(){return rejectionReason;} public void setRejectionReason(String v){rejectionReason=v;}
    public Instant getVerifiedAt(){return verifiedAt;} public void setVerifiedAt(Instant v){verifiedAt=v;} public String getVerifiedBy(){return verifiedBy;} public void setVerifiedBy(String v){verifiedBy=v;}
}

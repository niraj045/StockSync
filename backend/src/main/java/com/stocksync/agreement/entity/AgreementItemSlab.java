package com.stocksync.agreement.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "agreement_item_slabs")
public class AgreementItemSlab {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_item_id")
    private AgreementItem agreementItem;

    @Column(name = "start_day", nullable = false)
    private int startDay;

    @Column(name = "end_day")
    private Integer endDay;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal rate;

    @Version
    private long version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AgreementItem getAgreementItem() { return agreementItem; }
    public void setAgreementItem(AgreementItem agreementItem) { this.agreementItem = agreementItem; }
    public int getStartDay() { return startDay; }
    public void setStartDay(int startDay) { this.startDay = startDay; }
    public Integer getEndDay() { return endDay; }
    public void setEndDay(Integer endDay) { this.endDay = endDay; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}

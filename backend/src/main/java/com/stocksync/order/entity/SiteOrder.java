package com.stocksync.order.entity;
import com.stocksync.agreement.entity.Agreement;
import com.stocksync.common.persistence.AuditedEntity;
import com.stocksync.party.entity.Party;
import com.stocksync.site.entity.Site;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.*;
@Entity @Table(name="site_orders")
public class SiteOrder extends AuditedEntity{
    @Id@GeneratedValue(strategy=GenerationType.IDENTITY)private Long id;
    @Column(name="order_number",nullable=false,unique=true,length=50)private String orderNumber;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)@JoinColumn(name="agreement_id")private Agreement agreement;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)@JoinColumn(name="party_id")private Party party;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)@JoinColumn(name="site_id")private Site site;
    @Column(name="order_date",nullable=false)private LocalDate orderDate;
    @Enumerated(EnumType.STRING)@Column(nullable=false,length=20)private OrderStatus status=OrderStatus.DRAFT;
    @Column(length=1000)private String notes;
    @OneToMany(mappedBy="order",cascade=CascadeType.ALL,orphanRemoval=true)@OrderBy("id ASC")private List<SiteOrderItem>items=new ArrayList<>();
    public Long getId(){return id;}public String getOrderNumber(){return orderNumber;}public void setOrderNumber(String v){orderNumber=v;}
    public Agreement getAgreement(){return agreement;}public void setAgreement(Agreement v){agreement=v;}public Party getParty(){return party;}public void setParty(Party v){party=v;}
    public Site getSite(){return site;}public void setSite(Site v){site=v;}public LocalDate getOrderDate(){return orderDate;}public void setOrderDate(LocalDate v){orderDate=v;}
    public OrderStatus getStatus(){return status;}public void setStatus(OrderStatus v){status=v;}public String getNotes(){return notes;}public void setNotes(String v){notes=v;}
    public List<SiteOrderItem>getItems(){return items;}public void replaceItems(List<SiteOrderItem>v){items.clear();v.forEach(this::addItem);}
    public void addItem(SiteOrderItem i){i.setOrder(this);items.add(i);}
}

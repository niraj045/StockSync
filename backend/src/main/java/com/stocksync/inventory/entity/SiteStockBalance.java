package com.stocksync.inventory.entity;

import jakarta.persistence.*;
import com.stocksync.site.entity.Site;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "site_stock_balances", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"site_id", "item_id"})
})
public class SiteStockBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id")
    private Site site;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(name = "pending_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal pendingQuantity = BigDecimal.ZERO;

    @Version
    private long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Site getSite() { return site; }
    public void setSite(Site v) { site = v; }
    public Item getItem() { return item; }
    public void setItem(Item v) { item = v; }
    public BigDecimal getPendingQuantity() { return pendingQuantity; }
    public void setPendingQuantity(BigDecimal v) { pendingQuantity = v; }
    public long getVersion() { return version; }
    public Instant getUpdatedAt() { return updatedAt; }
}

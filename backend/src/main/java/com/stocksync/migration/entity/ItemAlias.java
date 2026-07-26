package com.stocksync.migration.entity;
import com.stocksync.inventory.entity.Item;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="item_aliases")
public class ItemAlias {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="item_id") private Item item;
 @Column(nullable=false) private String alias; @Column(name="source_system",nullable=false,length=80) private String sourceSystem;
 @Column(nullable=false) private boolean active=true; @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @PrePersist void create(){createdAt=Instant.now();}
 public Long getId(){return id;} public Item getItem(){return item;} public void setItem(Item v){item=v;}
 public String getAlias(){return alias;} public void setAlias(String v){alias=v;} public String getSourceSystem(){return sourceSystem;}
 public void setSourceSystem(String v){sourceSystem=v;} public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
 public Instant getCreatedAt(){return createdAt;}
}

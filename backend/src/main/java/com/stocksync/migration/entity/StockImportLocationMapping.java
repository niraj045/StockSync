package com.stocksync.migration.entity;
import com.stocksync.common.persistence.AuditedEntity;
import com.stocksync.party.entity.Party;
import com.stocksync.site.entity.Site;
import jakarta.persistence.*;
@Entity @Table(name="stock_import_location_mappings")
public class StockImportLocationMapping extends AuditedEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="batch_id") private StockImportBatch batch;
 @Column(name="source_excel_column",nullable=false,length=5) private String sourceExcelColumn;
 @Column(name="source_location_name",nullable=false) private String sourceLocationName;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="mapped_party_id") private Party mappedParty;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="mapped_site_id") private Site mappedSite;
 public Long getId(){return id;} public StockImportBatch getBatch(){return batch;} public void setBatch(StockImportBatch v){batch=v;}
 public String getSourceExcelColumn(){return sourceExcelColumn;} public void setSourceExcelColumn(String v){sourceExcelColumn=v;}
 public String getSourceLocationName(){return sourceLocationName;} public void setSourceLocationName(String v){sourceLocationName=v;}
 public Party getMappedParty(){return mappedParty;} public void setMappedParty(Party v){mappedParty=v;}
 public Site getMappedSite(){return mappedSite;} public void setMappedSite(Site v){mappedSite=v;}
}

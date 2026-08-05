package com.stocksync.inventory.service;

import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.inventory.entity.*;
import com.stocksync.inventory.repository.*;
import com.stocksync.party.repository.PartyRepository;
import com.stocksync.site.repository.SiteRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;import java.time.Instant;import java.util.Comparator;import java.util.Set;

@Service
public class OpeningStockService implements OpeningStockAccess {
    private final ItemRepository items;private final StockBalanceRepository balances;private final StockTransactionRepository transactions;
    private final SiteStockBalanceRepository siteBalances;private final PartyRepository parties;private final SiteRepository sites;private final JdbcTemplate jdbc;
    public OpeningStockService(ItemRepository items,StockBalanceRepository balances,StockTransactionRepository transactions,
            SiteStockBalanceRepository siteBalances,PartyRepository parties,SiteRepository sites,JdbcTemplate jdbc){
        this.items=items;this.balances=balances;this.transactions=transactions;this.siteBalances=siteBalances;this.parties=parties;this.sites=sites;this.jdbc=jdbc;}
    @Override @Transactional public Long post(OpeningCommand c){
        Item item=items.findById(c.itemId()).filter(Item::isActive)
            .orElseThrow(()->new BusinessRuleException("ITEM_NOT_FOUND_OR_INACTIVE","Active mapped item not found"));
        StockBalance balance=locked(item);apply(balance,c.stockBucket(),c.quantity(),true,item);
        applySitePending(item,c.siteId(),c.stockBucket(),c.quantity(),true);
        StockTransaction tx=base(item,c.transactionType(),c.stockBucket(),c.snapshotDate(),c.quantity(),"IN","STOCK_IMPORT",
            c.rowId() != null ? c.rowId() : -1L,c.batchId(),c.rowId(),c.partyId(),c.siteId(),c.sourceDescription(),c.actor());
        return transactions.save(tx).getId();
    }
    @Override @Transactional public Long reverse(ReversalCommand c){
        Item item=items.findById(c.itemId()).orElseThrow(()->new BusinessRuleException("ITEM_NOT_FOUND","Mapped item not found"));
        StockTransaction original=transactions.findById(c.originalTransactionId())
            .orElseThrow(()->new BusinessRuleException("IMPORT_TRANSACTION_NOT_FOUND","Original opening transaction not found"));
        StockBalance balance=locked(item);apply(balance,c.stockBucket(),c.quantity(),false,item);
        applySitePending(item,c.siteId(),c.stockBucket(),c.quantity(),false);
        String reversalType="AVAILABLE".equals(c.stockBucket())?"OPENING_GODOWN_REVERSAL":"OPENING_SITE_REVERSAL";
        StockTransaction tx=base(item,reversalType,c.stockBucket(),c.reversalDate(),c.quantity(),"OUT",
            "STOCK_IMPORT_REVERSAL",c.rowId() != null ? c.rowId() : -1L,c.batchId(),c.rowId(),c.partyId(),c.siteId(),c.reason(),c.actor());
        tx.setReversalOfTransaction(original);return transactions.save(tx).getId();
    }
    @Override @Transactional public void assertReversalSafe(Long batchId,Instant importedAt,Set<Long>itemIds){
        for(Long itemId:itemIds.stream().sorted(Comparator.naturalOrder()).toList()){
            balances.findForUpdate(itemId).orElseThrow(()->new BusinessRuleException("IMPORT_REVERSAL_UNSAFE",
                "Opening-stock balance no longer exists for item "+itemId));
            Integer count=jdbc.queryForObject("""
            SELECT COUNT(*) FROM stock_transactions
            WHERE item_id=? AND created_at>? AND (import_batch_id IS NULL OR import_batch_id<>?)
            """,Integer.class,itemId,importedAt,batchId);
            if(count!=null&&count>0)throw new BusinessRuleException("IMPORT_REVERSAL_UNSAFE",
                "Later stock transactions exist for item "+itemId+"; reverse or reconcile them first");}
    }
    private StockBalance locked(Item item){return balances.findForUpdate(item.getId()).orElseGet(()->{
        jdbc.update("INSERT IGNORE INTO stock_balances(item_id) VALUES(?)",item.getId());
        return balances.findForUpdate(item.getId()).orElseThrow();});}
    private void apply(StockBalance b,String bucket,BigDecimal quantity,boolean add,Item item){
        BigDecimal delta=add?quantity:quantity.negate();
        if("AVAILABLE".equals(bucket)){BigDecimal next=b.getAvailableQuantity().add(delta);if(next.signum()<0)throw insufficient(item,next);
            b.setAvailableQuantity(next);b.setAvailableWeight(next.multiply(item.getWeightPerPiece()==null?BigDecimal.ZERO:item.getWeightPerPiece()));}
        else if("ISSUED".equals(bucket)){BigDecimal next=b.getIssuedQuantity().add(delta);if(next.signum()<0)throw insufficient(item,next);b.setIssuedQuantity(next);}
        else throw new BusinessRuleException("UNSUPPORTED_STOCK_BUCKET","Opening stock bucket is not supported");
        balances.save(b);
    }
    private void applySitePending(Item item,Long siteId,String bucket,BigDecimal quantity,boolean add){
        if(!"ISSUED".equals(bucket))return;
        if(siteId==null)throw new BusinessRuleException("OPENING_SITE_REQUIRED","Opening site stock requires a mapped site");
        SiteStockBalance balance=siteBalances.findForUpdate(siteId,item.getId()).orElseGet(()->{
            SiteStockBalance created=new SiteStockBalance();
            created.setSite(sites.getReferenceById(siteId));created.setItem(item);
            return siteBalances.saveAndFlush(created);
        });
        BigDecimal next=balance.getPendingQuantity().add(add?quantity:quantity.negate());
        if(next.signum()<0)throw insufficient(item,next);
        balance.setPendingQuantity(next);siteBalances.save(balance);
    }
    private BusinessRuleException insufficient(Item item,BigDecimal next){return new BusinessRuleException("IMPORT_REVERSAL_UNSAFE",
        "Reversal would make stock negative for "+item.getItemCode()+" (result "+next+")");}
    private StockTransaction base(Item item,String type,String bucket,java.time.LocalDate date,BigDecimal quantity,String direction,
            String sourceType,Long sourceId,Long batchId,Long rowId,Long partyId,Long siteId,String notes,String actor){
        StockTransaction tx=new StockTransaction();tx.setItem(item);tx.setTransactionType(type);tx.setStockBucket(bucket);
        tx.setTransactionDate(date);tx.setQuantity(quantity);tx.setWeight(quantity.multiply(item.getWeightPerPiece()==null?BigDecimal.ZERO:item.getWeightPerPiece()));
        tx.setDirection(direction);tx.setSourceType(sourceType);tx.setSourceId(sourceId);tx.setImportBatchId(batchId);tx.setImportRowId(rowId);
        if(partyId!=null)tx.setParty(parties.getReferenceById(partyId));if(siteId!=null)tx.setSite(sites.getReferenceById(siteId));
        tx.setNotes(notes);tx.setCreatedBy(actor);return tx;
    }
}

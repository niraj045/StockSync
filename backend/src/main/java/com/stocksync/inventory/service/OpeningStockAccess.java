package com.stocksync.inventory.service;
import java.math.BigDecimal;import java.time.*;import java.util.Set;
public interface OpeningStockAccess {
    Long post(OpeningCommand command);
    Long reverse(ReversalCommand command);
    void assertReversalSafe(Long batchId,Instant importedAt,Set<Long>itemIds);
    record OpeningCommand(Long batchId,Long rowId,Long itemId,Long partyId,Long siteId,String transactionType,
        String stockBucket,LocalDate snapshotDate,BigDecimal quantity,String sourceDescription,String actor){}
    record ReversalCommand(Long batchId,Long rowId,Long originalTransactionId,Long itemId,Long partyId,Long siteId,
        String stockBucket,LocalDate reversalDate,BigDecimal quantity,String reason,String actor){}
}

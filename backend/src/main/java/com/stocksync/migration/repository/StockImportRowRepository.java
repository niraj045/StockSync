package com.stocksync.migration.repository;
import com.stocksync.migration.entity.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface StockImportRowRepository extends JpaRepository<StockImportRow,Long>,JpaSpecificationExecutor<StockImportRow>{
 Page<StockImportRow> findByBatchId(Long batchId,Pageable pageable);
 List<StockImportRow> findByBatchIdOrderBySourceExcelRowAscSourceExcelColumnAsc(Long batchId);
 List<StockImportRow> findByBatchIdAndSourceExcelRow(Long batchId,int sourceExcelRow);
 List<StockImportRow> findByBatchIdAndSourceExcelColumn(Long batchId,String sourceExcelColumn);
 Optional<StockImportRow> findByIdAndBatchId(Long id,Long batchId);
 long countByBatchIdAndValidationStatus(Long batchId,ImportValidationStatus status);
}

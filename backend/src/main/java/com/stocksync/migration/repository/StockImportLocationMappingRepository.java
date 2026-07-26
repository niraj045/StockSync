package com.stocksync.migration.repository;
import com.stocksync.migration.entity.StockImportLocationMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface StockImportLocationMappingRepository extends JpaRepository<StockImportLocationMapping,Long>{
 Optional<StockImportLocationMapping> findByBatchIdAndSourceExcelColumn(Long batchId,String column);
 List<StockImportLocationMapping> findByBatchIdOrderBySourceExcelColumn(Long batchId);
}

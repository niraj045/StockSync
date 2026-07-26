package com.stocksync.migration.repository;
import com.stocksync.migration.entity.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface StockImportBatchRepository extends JpaRepository<StockImportBatch,Long>,JpaSpecificationExecutor<StockImportBatch>{
 boolean existsByFileChecksum(String checksum); Optional<StockImportBatch> findByBatchCode(String code);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select b from StockImportBatch b where b.id=:id")
 Optional<StockImportBatch> findForUpdate(@Param("id")Long id);
}

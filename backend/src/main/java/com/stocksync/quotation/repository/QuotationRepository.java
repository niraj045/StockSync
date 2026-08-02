package com.stocksync.quotation.repository;

import com.stocksync.quotation.entity.Quotation;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface QuotationRepository extends JpaRepository<Quotation,Long>, JpaSpecificationExecutor<Quotation> {
    @EntityGraph(attributePaths = {"quotationTemplate","party","site","items","items.item","exactPdfAttachment"})
    Optional<Quotation> findDetailedById(Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"quotationTemplate","party","site","items","items.item","exactPdfAttachment"})
    @Query("select q from Quotation q where q.id=:id")
    Optional<Quotation> findDetailedForUpdate(@Param("id") Long id);
}

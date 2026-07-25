package com.stocksync.quotation.repository;

import com.stocksync.quotation.entity.Quotation;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

public interface QuotationRepository extends JpaRepository<Quotation,Long>, JpaSpecificationExecutor<Quotation> {
    @EntityGraph(attributePaths = {"party","site","items","items.item"})
    Optional<Quotation> findDetailedById(Long id);
}

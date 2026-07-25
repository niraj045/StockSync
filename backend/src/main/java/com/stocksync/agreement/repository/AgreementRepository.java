package com.stocksync.agreement.repository;
import com.stocksync.agreement.entity.Agreement;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
public interface AgreementRepository extends JpaRepository<Agreement,Long>,JpaSpecificationExecutor<Agreement>{
    @EntityGraph(attributePaths={"party","site","template","quotation","items","items.item"})
    Optional<Agreement> findDetailedById(Long id);
    boolean existsByQuotationId(Long quotationId);
}

package com.stocksync.agreement.repository;
import com.stocksync.agreement.entity.Agreement;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
public interface AgreementRepository extends JpaRepository<Agreement,Long>,JpaSpecificationExecutor<Agreement>{
    @EntityGraph(attributePaths={"party","site","template","quotation","generatedDocument","items","items.item","items.sourceQuotationItem"})
    Optional<Agreement> findDetailedById(Long id);
    boolean existsByQuotationId(Long quotationId);
    Optional<Agreement> findByQuotationId(Long quotationId);
    boolean existsBySiteIdAndStatusAndIdNot(Long siteId,com.stocksync.agreement.entity.AgreementStatus status,Long id);
    java.util.List<Agreement> findByStatus(com.stocksync.agreement.entity.AgreementStatus status);
}

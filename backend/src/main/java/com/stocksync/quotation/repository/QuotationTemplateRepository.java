package com.stocksync.quotation.repository;

import com.stocksync.quotation.entity.QuotationTemplate;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface QuotationTemplateRepository extends JpaRepository<QuotationTemplate, Long>, JpaSpecificationExecutor<QuotationTemplate> {
    boolean existsByTemplateCodeIgnoreCase(String templateCode);
    boolean existsByTemplateCodeIgnoreCaseAndIdNot(String templateCode, Long id);
    Optional<QuotationTemplate> findByIdAndActiveTrue(Long id);
}


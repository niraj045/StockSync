package com.stocksync.agreement.repository;
import com.stocksync.agreement.entity.AgreementTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AgreementTemplateRepository extends JpaRepository<AgreementTemplate,Long>{
    boolean existsByNameIgnoreCase(String name);
    List<AgreementTemplate> findAllByOrderByNameAsc();
}

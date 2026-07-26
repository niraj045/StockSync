package com.stocksync.site.repository;
import com.stocksync.site.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;
public interface SiteRepository extends JpaRepository<Site,Long>, JpaSpecificationExecutor<Site> {
    boolean existsBySiteCodeIgnoreCase(String code);
    boolean existsBySiteCodeIgnoreCaseAndIdNot(String code, Long id);
    boolean existsByPartyIdAndStatusNot(Long partyId, com.stocksync.site.entity.SiteStatus status);
    Optional<Site> findByIdAndPartyId(Long id,Long partyId);
}

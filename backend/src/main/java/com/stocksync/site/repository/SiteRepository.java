package com.stocksync.site.repository;
import com.stocksync.site.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;
public interface SiteRepository extends JpaRepository<Site,Long>, JpaSpecificationExecutor<Site> {
    boolean existsBySiteCodeIgnoreCase(String code);
    boolean existsBySiteCodeIgnoreCaseAndIdNot(String code, Long id);
    boolean existsByPartyIdAndStatusNot(Long partyId, com.stocksync.site.entity.SiteStatus status);
    Optional<Site> findByIdAndPartyId(Long id,Long partyId);
    Optional<Site> findBySiteCodeIgnoreCase(String code);
    Optional<Site> findBySiteNameIgnoreCase(String siteName);
    List<Site> findBySiteNameContainingIgnoreCase(String siteName);
    List<Site> findByPartyId(Long partyId);
}

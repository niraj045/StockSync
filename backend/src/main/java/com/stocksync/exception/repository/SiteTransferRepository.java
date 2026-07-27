package com.stocksync.exception.repository;

import com.stocksync.exception.entity.SiteTransfer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface SiteTransferRepository extends JpaRepository<SiteTransfer, Long>, JpaSpecificationExecutor<SiteTransfer> {

    @Query("select t from SiteTransfer t left join fetch t.sourceAgreement left join fetch t.destinationAgreement left join fetch t.sourceParty left join fetch t.sourceSite left join fetch t.destinationParty left join fetch t.destinationSite where t.id = :id")
    Optional<SiteTransfer> findByIdWithDetails(@Param("id") Long id);
}

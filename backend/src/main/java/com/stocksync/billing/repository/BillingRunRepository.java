package com.stocksync.billing.repository;

import com.stocksync.billing.entity.BillingRun;
import com.stocksync.billing.entity.BillingRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillingRunRepository extends JpaRepository<BillingRun, Long>, JpaSpecificationExecutor<BillingRun> {

    Optional<BillingRun> findByBillingRunNumberIgnoreCase(String billingRunNumber);

    @Query("SELECT b FROM BillingRun b LEFT JOIN FETCH b.segments WHERE b.id = :id")
    Optional<BillingRun> findDetailedById(@Param("id") Long id);

    List<BillingRun> findByAgreementIdAndStatus(Long agreementId, BillingRunStatus status);

    Optional<BillingRun> findTopByAgreementIdAndStatusNotOrderByPeriodEndDesc(
        Long agreementId,
        BillingRunStatus status
    );

    @Query("""
        SELECT COUNT(b) > 0 FROM BillingRun b 
        WHERE b.agreement.id = :agreementId 
          AND b.status <> :cancelledStatus
          AND ((b.periodStart <= :end AND b.periodEnd >= :start))
    """)
    boolean existsOverlappingPeriod(
        @Param("agreementId") Long agreementId,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end,
        @Param("cancelledStatus") BillingRunStatus cancelledStatus
    );
}

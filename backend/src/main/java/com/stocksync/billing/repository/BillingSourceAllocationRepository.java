package com.stocksync.billing.repository;

import com.stocksync.billing.entity.BillingSourceAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingSourceAllocationRepository extends JpaRepository<BillingSourceAllocation, Long> {
    boolean existsBySourceTypeAndSourceId(String sourceType, Long sourceId);
    List<BillingSourceAllocation> findByBillingRunId(Long billingRunId);
    void deleteByBillingRunId(Long billingRunId);
}

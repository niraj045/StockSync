package com.stocksync.billing.repository;

import com.stocksync.billing.entity.BillingRunSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillingRunSegmentRepository extends JpaRepository<BillingRunSegment, Long> {
}

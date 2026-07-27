package com.stocksync.billing.repository;

import com.stocksync.billing.entity.BillingRunCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillingRunChargeRepository extends JpaRepository<BillingRunCharge, Long> {
}

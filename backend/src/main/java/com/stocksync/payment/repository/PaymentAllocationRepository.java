package com.stocksync.payment.repository;

import com.stocksync.payment.entity.PaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;

@Repository
public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Long> {
    @Query("""
            SELECT COALESCE(SUM(a.cashAllocated + a.tdsAllocated), 0)
            FROM PaymentAllocation a
            WHERE a.paymentReceipt.id = :paymentId
            """)
    BigDecimal allocatedTotal(Long paymentId);
}

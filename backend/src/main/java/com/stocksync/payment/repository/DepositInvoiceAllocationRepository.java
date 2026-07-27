package com.stocksync.payment.repository;

import com.stocksync.payment.entity.DepositInvoiceAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepositInvoiceAllocationRepository extends JpaRepository<DepositInvoiceAllocation, Long> {}

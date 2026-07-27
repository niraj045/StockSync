package com.stocksync.payment.repository;

import com.stocksync.payment.entity.TdsDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TdsDetailsRepository extends JpaRepository<TdsDetails, Long> {
    Optional<TdsDetails> findByPaymentReceiptId(Long paymentReceiptId);
}

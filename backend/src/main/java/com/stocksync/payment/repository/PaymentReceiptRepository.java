package com.stocksync.payment.repository;

import com.stocksync.payment.entity.PaymentReceipt;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface PaymentReceiptRepository extends JpaRepository<PaymentReceipt, Long>, JpaSpecificationExecutor<PaymentReceipt> {
    @EntityGraph(attributePaths = {"party", "site", "allocations", "allocations.invoice", "tdsDetails", "attachment"})
    Optional<PaymentReceipt> findDetailedById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentReceipt p LEFT JOIN FETCH p.allocations WHERE p.id = :id")
    Optional<PaymentReceipt> findForUpdate(Long id);
}

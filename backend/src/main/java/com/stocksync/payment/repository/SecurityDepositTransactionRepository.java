package com.stocksync.payment.repository;

import com.stocksync.payment.entity.SecurityDepositTransaction;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface SecurityDepositTransactionRepository extends JpaRepository<SecurityDepositTransaction, Long>, JpaSpecificationExecutor<SecurityDepositTransaction> {
    @EntityGraph(attributePaths = {"agreement", "party", "site", "relatedInvoice", "sourceDepositTransaction"})
    Optional<SecurityDepositTransaction> findDetailedById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM SecurityDepositTransaction d WHERE d.id = :id")
    Optional<SecurityDepositTransaction> findForUpdate(Long id);

    @Query("""
            SELECT COALESCE(SUM(CASE
                WHEN d.status <> 'POSTED' THEN 0
                WHEN d.transactionType = 'RECEIPT' THEN d.amount
                WHEN d.transactionType IN ('REFUND','ADJUSTMENT_TO_INVOICE') THEN -d.amount
                ELSE 0 END), 0)
            FROM SecurityDepositTransaction d
            WHERE d.agreement.id = :agreementId
            """)
    BigDecimal availableForAgreement(Long agreementId);

    @Query("""
            SELECT COALESCE(SUM(CASE WHEN d.status = 'POSTED' AND d.transactionType = 'RECEIPT' THEN d.amount ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN d.status = 'POSTED' AND d.transactionType = 'ADJUSTMENT_TO_INVOICE' THEN d.amount ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN d.status = 'POSTED' AND d.transactionType = 'REFUND' THEN d.amount ELSE 0 END), 0)
            FROM SecurityDepositTransaction d
            WHERE d.agreement.id = :agreementId
            """)
    Object[] totalsForAgreement(Long agreementId);
}

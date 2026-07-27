package com.stocksync.agreement.repository;

import com.stocksync.agreement.entity.AgreementItemSlab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgreementItemSlabRepository extends JpaRepository<AgreementItemSlab, Long> {
    List<AgreementItemSlab> findByAgreementItemId(Long agreementItemId);
}

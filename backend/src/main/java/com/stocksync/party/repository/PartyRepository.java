package com.stocksync.party.repository;
import com.stocksync.party.entity.Party;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
public interface PartyRepository extends JpaRepository<Party, Long>, JpaSpecificationExecutor<Party> {
    boolean existsByGstinIgnoreCase(String gstin);
    boolean existsByGstinIgnoreCaseAndIdNot(String gstin, Long id);
    boolean existsByPanIgnoreCase(String pan);
    boolean existsByPanIgnoreCaseAndIdNot(String pan, Long id);
}

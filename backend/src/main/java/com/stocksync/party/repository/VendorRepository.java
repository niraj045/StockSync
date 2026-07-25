package com.stocksync.party.repository;
import com.stocksync.party.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
public interface VendorRepository extends JpaRepository<Vendor, Long>, JpaSpecificationExecutor<Vendor> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    boolean existsByGstinIgnoreCase(String gstin);
    boolean existsByGstinIgnoreCaseAndIdNot(String gstin, Long id);
}

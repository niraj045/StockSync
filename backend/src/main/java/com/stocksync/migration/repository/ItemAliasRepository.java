package com.stocksync.migration.repository;
import com.stocksync.migration.entity.ItemAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ItemAliasRepository extends JpaRepository<ItemAlias,Long>{
 Optional<ItemAlias> findByAliasIgnoreCaseAndSourceSystemAndActiveTrue(String alias,String sourceSystem);
 boolean existsByAliasIgnoreCaseAndSourceSystem(String alias,String sourceSystem);
}

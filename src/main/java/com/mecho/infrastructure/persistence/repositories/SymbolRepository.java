package com.mecho.infrastructure.persistence.repositories;

import com.mecho.infrastructure.persistence.entities.SymbolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SymbolRepository extends JpaRepository<SymbolEntity, Long> {
    
    Optional<SymbolEntity> findByTicker(String ticker);
    
    List<SymbolEntity> findByEnabled(Boolean enabled);
    
    boolean existsByTicker(String ticker);
}

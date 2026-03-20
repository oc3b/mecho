package com.mecho.infrastructure.persistence.repositories;

import com.mecho.infrastructure.persistence.entities.PredictionEntity;
import com.mecho.infrastructure.persistence.entities.SymbolEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PredictionRepository extends JpaRepository<PredictionEntity, Long> {
    
    List<PredictionEntity> findBySymbolOrderByTimestampDesc(SymbolEntity symbol, Pageable pageable);
    
    Optional<PredictionEntity> findTopBySymbolOrderByTimestampDesc(SymbolEntity symbol);
    
    List<PredictionEntity> findBySymbolOrderByCreatedAtDesc(SymbolEntity symbol, Pageable pageable);
}

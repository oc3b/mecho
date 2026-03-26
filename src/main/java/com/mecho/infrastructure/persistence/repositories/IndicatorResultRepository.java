package com.mecho.infrastructure.persistence.repositories;

import com.mecho.infrastructure.persistence.entities.IndicatorResultEntity;
import com.mecho.infrastructure.persistence.entities.SymbolEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface IndicatorResultRepository extends JpaRepository<IndicatorResultEntity, Long> {
    
    List<IndicatorResultEntity> findBySymbolAndIndicatorCodeOrderByTimestampDesc(
            SymbolEntity symbol,
            String indicatorCode,
            Pageable pageable
    );
    
    Optional<IndicatorResultEntity> findTopBySymbolAndIndicatorCodeOrderByTimestampDesc(
            SymbolEntity symbol,
            String indicatorCode
    );
    
    List<IndicatorResultEntity> findBySymbolAndTimestampBetweenOrderByTimestampAsc(
            SymbolEntity symbol,
            Instant startTime,
            Instant endTime
    );
    
    Optional<IndicatorResultEntity> findTopBySymbolIdAndIndicatorCodeOrderByIdDesc(
            Long symbolId,
            String indicatorCode
    );
    
    List<IndicatorResultEntity> findBySymbolOrderByTimestampDesc(SymbolEntity symbol, Pageable pageable);
}

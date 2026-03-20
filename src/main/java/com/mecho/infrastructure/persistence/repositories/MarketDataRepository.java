package com.mecho.infrastructure.persistence.repositories;

import com.mecho.infrastructure.persistence.entities.MarketDataEntity;
import com.mecho.infrastructure.persistence.entities.SymbolEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MarketDataRepository extends JpaRepository<MarketDataEntity, Long> {
    
    List<MarketDataEntity> findBySymbolOrderByTimestampDesc(SymbolEntity symbol, Pageable pageable);
    
    List<MarketDataEntity> findBySymbolAndTimestampBetweenOrderByTimestampAsc(
            SymbolEntity symbol,
            Instant startTime,
            Instant endTime
    );
    
    @Query("SELECT m FROM MarketDataEntity m WHERE m.symbol = :symbol ORDER BY m.timestamp DESC LIMIT 1")
    MarketDataEntity findLatestBySymbol(@Param("symbol") SymbolEntity symbol);
    
    boolean existsBySymbolAndTimestamp(SymbolEntity symbol, Instant timestamp);
}

package com.mecho.infrastructure.api;

import com.mecho.domain.model.OHLCV;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface MarketDataApi {
    
    CompletableFuture<List<OHLCV>> fetchHistorical(String symbol, Instant from, Instant to);
    
    CompletableFuture<OHLCV> fetchLatest(String symbol);
    
    CompletableFuture<List<OHLCV>> fetchDaily(String symbol, int days);
    
    String getSource();
}

package com.mecho.application.service;

import com.mecho.application.config.ConfigLoader;
import com.mecho.application.config.MarketDataConfig;
import com.mecho.domain.model.MarketData;
import com.mecho.domain.model.OHLCV;
import com.mecho.domain.model.Symbol;
import com.mecho.infrastructure.api.MarketDataApi;
import com.mecho.infrastructure.persistence.entities.MarketDataEntity;
import com.mecho.infrastructure.persistence.entities.SymbolEntity;
import com.mecho.infrastructure.persistence.repositories.MarketDataRepository;
import com.mecho.infrastructure.persistence.repositories.SymbolRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FetchMarketDataService {
    
    private static final Logger log = LoggerFactory.getLogger(FetchMarketDataService.class);
    
    private final MarketDataRepository marketDataRepository;
    private final SymbolRepository symbolRepository;
    private final Map<String, MarketDataApi> marketDataApis;
    private final ConfigLoader configLoader;
    private final MarketDataConfig marketDataConfig;
    
    public FetchMarketDataService(
            MarketDataRepository marketDataRepository,
            SymbolRepository symbolRepository,
            List<MarketDataApi> marketDataApis,
            ConfigLoader configLoader,
            MarketDataConfig marketDataConfig) {
        this.marketDataRepository = marketDataRepository;
        this.symbolRepository = symbolRepository;
        this.configLoader = configLoader;
        this.marketDataConfig = marketDataConfig;
        
        this.marketDataApis = new ConcurrentHashMap<>();
        for (MarketDataApi api : marketDataApis) {
            this.marketDataApis.put(api.getSource(), api);
        }
    }
    
    @Transactional
    public void fetchDataForSymbol(String ticker) {
        SymbolEntity symbolEntity = getOrCreateSymbol(ticker);
        MarketDataApi api = selectApiForSymbol(symbolEntity);
        
        if (api == null) {
            log.warn("No API available for symbol: {}", ticker);
            return;
        }
        
        int days = marketDataConfig.getHistoryDays();
        Instant from = Instant.now().minus(days, ChronoUnit.DAYS);
        Instant to = Instant.now();
        
        try {
            List<OHLCV> historicalData = api.fetchHistorical(ticker, from, to).join();
            
            int savedCount = 0;
            for (OHLCV ohlcv : historicalData) {
                if (!marketDataRepository.existsBySymbolAndTimestamp(symbolEntity, ohlcv.timestamp())) {
                    MarketDataEntity entity = toEntity(symbolEntity, ohlcv, api.getSource());
                    marketDataRepository.save(entity);
                    savedCount++;
                }
            }
            
            log.info("Fetched {} data points for {}, saved {} new records", 
                    historicalData.size(), ticker, savedCount);
            
        } catch (Exception e) {
            log.error("Error fetching historical data for {}: {} - Full stack trace:", ticker, e.getMessage(), e);
        }
    }
    
    private SymbolEntity getOrCreateSymbol(String ticker) {
        return symbolRepository.findByTicker(ticker)
                .orElseGet(() -> {
                    SymbolEntity newSymbol = SymbolEntity.builder()
                            .ticker(ticker)
                            .name(ticker)
                            .enabled(true)
                            .build();
                    return symbolRepository.save(newSymbol);
                });
    }
    
    private MarketDataApi selectApiForSymbol(SymbolEntity symbol) {
        String provider = determineProvider(symbol);
        MarketDataApi api = marketDataApis.get(provider);
        log.debug("Selected API '{}' for symbol '{}' (available APIs: {})", 
                provider, symbol.getTicker(), marketDataApis.keySet());
        return api;
    }
    
    private String determineProvider(SymbolEntity symbol) {
        String ticker = symbol.getTicker().toLowerCase();

        if (configLoader.isCryptoSymbol(ticker)) {
            log.debug("Symbol '{}' detected as crypto, routing to COIN_GECKO", ticker);
            return "COIN_GECKO";
        }

        // Stocks: Always use YFinance as primary provider (no rate limits)
        log.debug("Symbol '{}' detected as stock, routing to YFINANCE", ticker);
        return "YFINANCE";
    }
    
    private MarketDataEntity toEntity(SymbolEntity symbol, OHLCV ohlcv, String source) {
        MarketDataEntity.DataSource dataSource;
        try {
            dataSource = MarketDataEntity.DataSource.valueOf(source);
        } catch (IllegalArgumentException e) {
            dataSource = MarketDataEntity.DataSource.CUSTOM;
        }
        
        BigDecimal volume = ohlcv.volume();
        if (volume == null) {
            volume = BigDecimal.ZERO;
        }
        
        return MarketDataEntity.builder()
                .symbol(symbol)
                .timestamp(ohlcv.timestamp())
                .openPrice(ohlcv.open())
                .highPrice(ohlcv.high())
                .lowPrice(ohlcv.low())
                .closePrice(ohlcv.close())
                .volume(volume)
                .dataSource(dataSource)
                .build();
    }
    
    private List<String> getSymbolsToFetch() {
        List<String> configSymbols = configLoader.getSymbols();
        if (configSymbols != null && !configSymbols.isEmpty()) {
            return configSymbols;
        }
        
        List<String> yamlSymbols = marketDataConfig.getSymbols();
        if (yamlSymbols != null && !yamlSymbols.isEmpty()) {
            return yamlSymbols;
        }
        
        return List.of("AAPL", "MSFT", "GOOGL");
    }
    
    @Transactional(readOnly = true)
    public List<MarketData> getRecentMarketData(String ticker, int limit) {
        return symbolRepository.findByTicker(ticker)
                .map(symbol -> marketDataRepository
                        .findBySymbolOrderByTimestampDesc(symbol, 
                                org.springframework.data.domain.PageRequest.of(0, limit)))
                .map(entities -> entities.stream()
                        .map(this::toDomain)
                        .toList())
                .orElse(List.of());
    }
    
    private MarketData toDomain(MarketDataEntity entity) {
        return new MarketData(
                entity.getId(),
                toSymbolDomain(entity.getSymbol()),
                entity.getTimestamp(),
                entity.getOpenPrice(),
                entity.getHighPrice(),
                entity.getLowPrice(),
                entity.getClosePrice(),
                entity.getVolume(),
                entity.getDataSource().name()
        );
    }
    
    private Symbol toSymbolDomain(SymbolEntity entity) {
        return Symbol.builder()
                .id(entity.getId())
                .ticker(entity.getTicker())
                .name(entity.getName())
                .assetClass(entity.getAssetClass())
                .exchange(entity.getExchange())
                .enabled(entity.getEnabled())
                .build();
    }
}

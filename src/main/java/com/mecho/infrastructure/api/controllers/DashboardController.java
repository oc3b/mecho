package com.mecho.infrastructure.api.controllers;

import com.mecho.infrastructure.api.dto.*;
import com.mecho.infrastructure.persistence.entities.IndicatorResultEntity;
import com.mecho.infrastructure.persistence.entities.MarketDataEntity;
import com.mecho.infrastructure.persistence.entities.PredictionEntity;
import com.mecho.infrastructure.persistence.entities.SymbolEntity;
import com.mecho.infrastructure.persistence.repositories.IndicatorResultRepository;
import com.mecho.infrastructure.persistence.repositories.MarketDataRepository;
import com.mecho.infrastructure.persistence.repositories.PredictionRepository;
import com.mecho.infrastructure.persistence.repositories.SymbolRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
    private static final BigDecimal PREDICTION_THRESHOLD = new BigDecimal("0.6");

    private final SymbolRepository symbolRepository;
    private final MarketDataRepository marketDataRepository;
    private final IndicatorResultRepository indicatorResultRepository;
    private final PredictionRepository predictionRepository;
    private final Instant applicationStartTime;

    public DashboardController(
            SymbolRepository symbolRepository,
            MarketDataRepository marketDataRepository,
            IndicatorResultRepository indicatorResultRepository,
            PredictionRepository predictionRepository,
            @Value("${mecho.start-time:#{null}}") Instant startTime) {
        this.symbolRepository = symbolRepository;
        this.marketDataRepository = marketDataRepository;
        this.indicatorResultRepository = indicatorResultRepository;
        this.predictionRepository = predictionRepository;
        this.applicationStartTime = startTime != null ? startTime : Instant.now();
    }

    @GetMapping("/status")
    @Transactional(readOnly = true)
    public StatusInfo getStatus() {
        log.debug("Fetching application status");
        
        List<SymbolEntity> allSymbols = symbolRepository.findAll();
        int symbolsWithData = 0;
        Instant lastSync = null;
        
        for (SymbolEntity symbol : allSymbols) {
            MarketDataEntity latest = marketDataRepository.findLatestBySymbol(symbol);
            if (latest != null) {
                symbolsWithData++;
                if (lastSync == null || latest.getTimestamp().isAfter(lastSync)) {
                    lastSync = latest.getTimestamp();
                }
            }
        }
        
        long uptimeSeconds = Instant.now().getEpochSecond() - applicationStartTime.getEpochSecond();
        
        return StatusInfo.builder()
                .status("RUNNING")
                .lastSync(lastSync != null ? LocalDateTime.ofInstant(lastSync, ZoneId.systemDefault()) : null)
                .totalSymbols(allSymbols.size())
                .symbolsWithData(symbolsWithData)
                .uptime(uptimeSeconds)
                .build();
    }

    @GetMapping("/symbols")
    @Transactional(readOnly = true)
    public List<SymbolInfo> getAllSymbols() {
        log.debug("Fetching all symbols");
        
        List<SymbolEntity> symbols = symbolRepository.findAllByOrderByTicker();
        List<SymbolInfo> result = new ArrayList<>();
        
        for (SymbolEntity symbol : symbols) {
            MarketDataEntity latest = marketDataRepository.findLatestBySymbol(symbol);
            long dataPointsCount = marketDataRepository.countBySymbol(symbol);
            
            result.add(SymbolInfo.builder()
                    .ticker(symbol.getTicker())
                    .assetClass(symbol.getAssetClass() != null ? symbol.getAssetClass().name() : null)
                    .enabled(symbol.getEnabled())
                    .lastFetchTime(latest != null ? 
                            LocalDateTime.ofInstant(latest.getTimestamp(), ZoneId.systemDefault()) : null)
                    .dataPointsCount((int) dataPointsCount)
                    .success(latest != null)
                    .build());
        }
        
        return result;
    }

    @GetMapping("/symbols/{ticker}")
    @Transactional(readOnly = true)
    public SymbolDetail getSymbolDetail(@PathVariable String ticker) {
        log.debug("Fetching symbol detail for: {}", ticker);
        
        SymbolEntity symbol = symbolRepository.findByTicker(ticker)
                .orElseThrow(() -> new IllegalArgumentException("Symbol not found: " + ticker));
        
        MarketDataEntity latest = marketDataRepository.findLatestBySymbol(symbol);
        
        List<MarketDataEntity> recentData = marketDataRepository
                .findBySymbolOrderByTimestampDesc(symbol, PageRequest.of(0, 100));
        
        List<MarketDataDTO> marketDataDTOs = recentData.stream()
                .map(this::toMarketDataDTO)
                .toList();
        
        return SymbolDetail.builder()
                .ticker(symbol.getTicker())
                .name(symbol.getName())
                .assetClass(symbol.getAssetClass() != null ? symbol.getAssetClass().name() : null)
                .latestPrice(latest != null ? latest.getClosePrice() : null)
                .latestVolume(latest != null ? latest.getVolume() : null)
                .latestTimestamp(latest != null ? 
                        LocalDateTime.ofInstant(latest.getTimestamp(), ZoneId.systemDefault()) : null)
                .marketData(marketDataDTOs)
                .build();
    }

    @GetMapping("/symbols/{ticker}/data")
    @Transactional(readOnly = true)
    public SymbolDataResponse getSymbolData(@PathVariable String ticker) {
        log.debug("Fetching symbol data for chart: {}", ticker);
        
        SymbolEntity symbol = symbolRepository.findByTicker(ticker)
                .orElseThrow(() -> new IllegalArgumentException("Symbol not found: " + ticker));
        
        List<MarketDataEntity> marketData = marketDataRepository
                .findTop100BySymbolOrderByTimestampDesc(symbol);
        
        List<MarketDataDTO> dataPoints = marketData.stream()
                .map(this::toMarketDataDTO)
                .toList();
        
        return SymbolDataResponse.builder()
                .ticker(ticker)
                .data(dataPoints)
                .build();
    }

    @GetMapping("/symbols/{ticker}/indicators")
    @Transactional(readOnly = true)
    public IndicatorInfo getSymbolIndicators(@PathVariable String ticker) {
        log.debug("Fetching indicators for: {}", ticker);
        
        SymbolEntity symbol = symbolRepository.findByTicker(ticker)
                .orElseThrow(() -> new IllegalArgumentException("Symbol not found: " + ticker));
        
        Map<String, BigDecimal> indicatorValues = new HashMap<>();
        Instant latestTimestamp = null;
        
        indicatorValues.put("RSI", getIndicatorValue(symbol, "RSI"));
        indicatorValues.put("MACD", getIndicatorValue(symbol, "MACD"));
        indicatorValues.put("BB_UPPER", getIndicatorValue(symbol, "BB_UPPER"));
        indicatorValues.put("BB_LOWER", getIndicatorValue(symbol, "BB_LOWER"));
        indicatorValues.put("MA50", getIndicatorValue(symbol, "MA50"));
        indicatorValues.put("MA200", getIndicatorValue(symbol, "MA200"));
        indicatorValues.put("STOCH", getIndicatorValue(symbol, "STOCH"));
        
        Optional<IndicatorResultEntity> rsiEntity = indicatorResultRepository
                .findTopBySymbolAndIndicatorCodeOrderByTimestampDesc(symbol, "RSI");
        if (rsiEntity.isPresent()) {
            latestTimestamp = rsiEntity.get().getTimestamp();
        }
        
        return IndicatorInfo.builder()
                .ticker(ticker)
                .timestamp(latestTimestamp != null ? 
                        LocalDateTime.ofInstant(latestTimestamp, ZoneId.systemDefault()) : null)
                .rsi(indicatorValues.get("RSI"))
                .macd(indicatorValues.get("MACD"))
                .upperBand(indicatorValues.get("BB_UPPER"))
                .lowerBand(indicatorValues.get("BB_LOWER"))
                .ma50(indicatorValues.get("MA50"))
                .ma200(indicatorValues.get("MA200"))
                .stoch(indicatorValues.get("STOCH"))
                .build();
    }

    @GetMapping("/predictions")
    @Transactional(readOnly = true)
    public List<PredictionInfo> getPredictions() {
        log.debug("Fetching latest predictions");
        
        List<PredictionEntity> allPredictions = predictionRepository.findAll();
        List<PredictionInfo> result = new ArrayList<>();
        
        Map<Long, PredictionEntity> latestBySymbol = new HashMap<>();
        for (PredictionEntity prediction : allPredictions) {
            Long symbolId = prediction.getSymbol().getId();
            if (!latestBySymbol.containsKey(symbolId) ||
                    prediction.getTimestamp().isAfter(latestBySymbol.get(symbolId).getTimestamp())) {
                latestBySymbol.put(symbolId, prediction);
            }
        }
        
        for (PredictionEntity prediction : latestBySymbol.values()) {
            if (prediction.getProbability().compareTo(PREDICTION_THRESHOLD) >= 0) {
                Map<String, Double> indicators = new HashMap<>();
                
                List<IndicatorResultEntity> indicatorResults = indicatorResultRepository
                        .findBySymbolOrderByTimestampDesc(
                                prediction.getSymbol(), 
                                PageRequest.of(0, 50)
                        );
                
                for (IndicatorResultEntity ind : indicatorResults) {
                    if (!indicators.containsKey(ind.getIndicatorCode())) {
                        indicators.put(ind.getIndicatorCode(), ind.getValue().doubleValue());
                    }
                }
                
                String direction = switch (prediction.getDirection()) {
                    case UP -> "UP";
                    case DOWN -> "DOWN";
                    case SIDEWAYS -> "NEUTRAL";
                };
                
                result.add(PredictionInfo.builder()
                        .ticker(prediction.getSymbol().getTicker())
                        .direction(direction)
                        .probability(prediction.getProbability())
                        .timestamp(LocalDateTime.ofInstant(prediction.getTimestamp(), ZoneId.systemDefault()))
                        .indicators(indicators)
                        .build());
            }
        }
        
        result.sort(Comparator.comparing(PredictionInfo::getTimestamp).reversed());
        return result;
    }

    private BigDecimal getIndicatorValue(SymbolEntity symbol, String indicatorCode) {
        return indicatorResultRepository
                .findTopBySymbolAndIndicatorCodeOrderByTimestampDesc(symbol, indicatorCode)
                .map(IndicatorResultEntity::getValue)
                .orElse(null);
    }

    private MarketDataDTO toMarketDataDTO(MarketDataEntity entity) {
        return MarketDataDTO.builder()
                .timestamp(LocalDateTime.ofInstant(entity.getTimestamp(), ZoneId.systemDefault()))
                .open(entity.getOpenPrice())
                .high(entity.getHighPrice())
                .low(entity.getLowPrice())
                .close(entity.getClosePrice())
                .volume(entity.getVolume())
                .build();
    }
}

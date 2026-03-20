package com.mecho.application.service;

import com.mecho.domain.indicators.Indicator;
import com.mecho.domain.indicators.Signal;
import com.mecho.domain.model.OHLCV;
import com.mecho.domain.model.Symbol;
import com.mecho.domain.prediction.Prediction;
import com.mecho.domain.prediction.WeightedPredictionCalculator;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyzeIndicatorsService {
    
    private static final Logger log = LoggerFactory.getLogger(AnalyzeIndicatorsService.class);
    
    private final Map<String, Indicator> indicators;
    private final Map<String, Double> indicatorWeights;
    private final MarketDataRepository marketDataRepository;
    private final IndicatorResultRepository indicatorResultRepository;
    private final PredictionRepository predictionRepository;
    private final SymbolRepository symbolRepository;
    private final WeightedPredictionCalculator predictionCalculator;
    
    public AnalyzeIndicatorsService(
            Map<String, Indicator> indicators,
            Map<String, Double> indicatorWeights,
            MarketDataRepository marketDataRepository,
            IndicatorResultRepository indicatorResultRepository,
            PredictionRepository predictionRepository,
            SymbolRepository symbolRepository) {
        this.indicators = indicators;
        this.indicatorWeights = indicatorWeights;
        this.marketDataRepository = marketDataRepository;
        this.indicatorResultRepository = indicatorResultRepository;
        this.predictionRepository = predictionRepository;
        this.symbolRepository = symbolRepository;
        this.predictionCalculator = new WeightedPredictionCalculator(indicatorWeights);
    }
    
    @Transactional
    public Prediction analyzeSymbol(String ticker) {
        log.info("Analyzing indicators for symbol: {}", ticker);
        
        SymbolEntity symbolEntity = symbolRepository.findByTicker(ticker)
                .orElseThrow(() -> new IllegalArgumentException("Symbol not found: " + ticker));
        
        List<MarketDataEntity> marketDataEntities = marketDataRepository
                .findBySymbolOrderByTimestampDesc(symbolEntity, PageRequest.of(0, 200));
        
        if (marketDataEntities.size() < 50) {
            log.warn("Insufficient market data for {}: {} records", ticker, marketDataEntities.size());
            return Prediction.create(toSymbolDomain(symbolEntity), 
                    com.mecho.domain.model.PredictionDirection.NEUTRAL, BigDecimal.ZERO);
        }
        
        List<OHLCV> prices = new ArrayList<>();
        for (int i = marketDataEntities.size() - 1; i >= 0; i--) {
            prices.add(toOHLCV(marketDataEntities.get(i)));
        }
        
        Map<String, Double> indicatorResults = calculateAllIndicators(prices);
        
        saveIndicatorResults(symbolEntity, indicatorResults);
        
        Prediction prediction = predictionCalculator.calculate(
                toSymbolDomain(symbolEntity),
                prices,
                new ArrayList<>(indicators.values())
        );
        
        savePrediction(symbolEntity, prediction);
        
        log.info("Analysis complete for {}: direction={}, probability={}", 
                ticker, prediction.direction(), prediction.probability());
        
        return prediction;
    }
    
    public Map<String, Double> calculateAllIndicators(List<OHLCV> prices) {
        Map<String, Double> results = new HashMap<>();
        
        if (prices == null || prices.isEmpty()) {
            return results;
        }
        
        for (Map.Entry<String, Indicator> entry : indicators.entrySet()) {
            try {
                double value = entry.getValue().calculate(prices);
                results.put(entry.getKey(), value);
                log.debug("{} indicator value: {}", entry.getKey(), value);
            } catch (Exception e) {
                log.error("Error calculating {}: {}", entry.getKey(), e.getMessage());
                results.put(entry.getKey(), 0.0);
            }
        }
        
        return results;
    }
    
    @Transactional
    public void saveIndicatorResults(SymbolEntity symbol, Map<String, Double> results) {
        Instant now = Instant.now();
        
        for (Map.Entry<String, Double> entry : results.entrySet()) {
            IndicatorResultEntity entity = IndicatorResultEntity.builder()
                    .symbol(symbol)
                    .indicatorCode(entry.getKey())
                    .value(BigDecimal.valueOf(entry.getValue()))
                    .timestamp(now)
                    .signal(toSignal(entry.getKey(), entry.getValue()))
                    .build();
            
            indicatorResultRepository.save(entity);
        }
    }
    
    private IndicatorResultEntity.Signal toSignal(String indicatorCode, Double value) {
        Indicator indicator = indicators.get(indicatorCode);
        if (indicator == null) return IndicatorResultEntity.Signal.NEUTRAL;
        
        Signal signal = indicator.calculateSignal(value);
        return switch (signal) {
            case BUY -> IndicatorResultEntity.Signal.BUY;
            case SELL -> IndicatorResultEntity.Signal.SELL;
            case NEUTRAL -> IndicatorResultEntity.Signal.NEUTRAL;
        };
    }
    
    @Transactional
    public void savePrediction(SymbolEntity symbol, Prediction prediction) {
        PredictionEntity.Direction direction = switch (prediction.direction()) {
            case UP -> PredictionEntity.Direction.UP;
            case DOWN -> PredictionEntity.Direction.DOWN;
            case NEUTRAL -> PredictionEntity.Direction.SIDEWAYS;
        };
        
        PredictionEntity entity = PredictionEntity.builder()
                .symbol(symbol)
                .timestamp(Instant.now())
                .direction(direction)
                .probability(prediction.probability())
                .createdAt(Instant.now())
                .build();
        
        predictionRepository.save(entity);
    }
    
    @Transactional(readOnly = true)
    public Map<String, Double> getLatestIndicatorValues(String ticker) {
        SymbolEntity symbol = symbolRepository.findByTicker(ticker)
                .orElseThrow(() -> new IllegalArgumentException("Symbol not found: " + ticker));
        
        Map<String, Double> latestValues = new HashMap<>();
        
        for (String code : indicators.keySet()) {
            indicatorResultRepository
                    .findTopBySymbolAndIndicatorCodeOrderByTimestampDesc(symbol, code)
                    .ifPresent(entity -> latestValues.put(code, entity.getValue().doubleValue()));
        }
        
        return latestValues;
    }
    
    private OHLCV toOHLCV(MarketDataEntity entity) {
        return new OHLCV(
                entity.getOpenPrice() != null ? entity.getOpenPrice() : BigDecimal.ZERO,
                entity.getHighPrice() != null ? entity.getHighPrice() : BigDecimal.ZERO,
                entity.getLowPrice() != null ? entity.getLowPrice() : BigDecimal.ZERO,
                entity.getClosePrice() != null ? entity.getClosePrice() : BigDecimal.ZERO,
                entity.getVolume() != null ? entity.getVolume() : BigDecimal.ZERO,
                entity.getTimestamp()
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

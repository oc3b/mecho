package com.mecho.domain.prediction;

import com.mecho.domain.indicators.Indicator;
import com.mecho.domain.indicators.Signal;
import com.mecho.domain.model.OHLCV;
import com.mecho.domain.model.PredictionDirection;
import com.mecho.domain.model.Symbol;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeightedPredictionCalculator {
    
    private final Map<String, Double> indicatorWeights;
    
    public WeightedPredictionCalculator() {
        this.indicatorWeights = new HashMap<>();
        this.indicatorWeights.put("RSI", 0.20);
        this.indicatorWeights.put("MACD", 0.25);
        this.indicatorWeights.put("BB", 0.20);
        this.indicatorWeights.put("MACROSS", 0.25);
        this.indicatorWeights.put("STOCH", 0.10);
    }
    
    public WeightedPredictionCalculator(Map<String, Double> weights) {
        this.indicatorWeights = new HashMap<>(weights);
    }
    
    public Prediction calculate(Symbol symbol, List<OHLCV> prices, List<Indicator> indicators) {
        if (prices == null || prices.isEmpty() || indicators == null || indicators.isEmpty()) {
            return Prediction.create(symbol, PredictionDirection.NEUTRAL, BigDecimal.ZERO);
        }
        
        double weightedScore = 0.0;
        double totalWeight = 0.0;
        
        for (Indicator indicator : indicators) {
            String code = indicator.getCode();
            Double weight = indicatorWeights.getOrDefault(code, 0.0);
            
            if (weight > 0) {
                double value = indicator.calculate(prices);
                Signal signal = indicator.calculateSignal(value);
                
                double signalScore = switch (indicator.getClass().getSimpleName()) {
                    case "RSIIndicator" -> switch (signal) {
                        case BUY -> -0.5;
                        case SELL -> 1.0;
                        case NEUTRAL -> 0.0;
                    };
                    default -> switch (signal) {
                        case BUY -> 1.0;
                        case SELL -> -1.0;
                        case NEUTRAL -> 0.0;
                    };
                };
                
                weightedScore += signalScore * weight;
                totalWeight += weight;
            }
        }
        
        if (totalWeight == 0) {
            return Prediction.create(symbol, PredictionDirection.NEUTRAL, BigDecimal.ZERO);
        }
        
        double normalizedScore = weightedScore / totalWeight;
        BigDecimal probability = BigDecimal.valueOf(Math.abs(normalizedScore))
            .setScale(4, RoundingMode.HALF_UP);
        
        PredictionDirection direction;
        if (normalizedScore > 0.0) {
            direction = PredictionDirection.UP;
        } else if (normalizedScore < 0.0) {
            direction = PredictionDirection.DOWN;
        } else {
            direction = PredictionDirection.NEUTRAL;
        }
        
        return Prediction.create(symbol, direction, probability);
    }
    
    public void setWeight(String indicatorCode, double weight) {
        indicatorWeights.put(indicatorCode, weight);
    }
    
    public Double getWeight(String indicatorCode) {
        return indicatorWeights.get(indicatorCode);
    }
}

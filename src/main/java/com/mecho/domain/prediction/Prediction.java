package com.mecho.domain.prediction;

import com.mecho.domain.model.PredictionDirection;
import com.mecho.domain.model.Symbol;
import java.math.BigDecimal;
import java.time.Instant;

public record Prediction(
    Long id,
    Symbol symbol,
    Instant timestamp,
    PredictionDirection direction,
    BigDecimal probability,
    BigDecimal confidence
) {
    public Prediction {
        if (probability != null) {
            probability = probability.min(BigDecimal.ONE).max(BigDecimal.ZERO);
        }
        if (confidence != null) {
            confidence = confidence.min(BigDecimal.ONE).max(BigDecimal.ZERO);
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
    
    public static Prediction create(Symbol symbol, PredictionDirection direction, BigDecimal probability) {
        return new Prediction(null, symbol, Instant.now(), direction, probability, null);
    }
    
    public static Prediction create(Symbol symbol, PredictionDirection direction, BigDecimal probability, BigDecimal confidence) {
        return new Prediction(null, symbol, Instant.now(), direction, probability, confidence);
    }
}

package com.mecho.domain.prediction;

import com.mecho.domain.model.PredictionDirection;
import com.mecho.domain.model.Symbol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PredictionTest {

    private Symbol createTestSymbol() {
        return Symbol.builder()
                .id(1L)
                .ticker("AAPL")
                .name("Apple Inc.")
                .build();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create Prediction with all values")
        void shouldCreatePredictionWithAllValues() {
            Symbol symbol = createTestSymbol();
            Instant timestamp = Instant.now();
            PredictionDirection direction = PredictionDirection.UP;
            BigDecimal probability = BigDecimal.valueOf(0.75);
            BigDecimal confidence = BigDecimal.valueOf(0.85);

            Prediction prediction = new Prediction(1L, symbol, timestamp, direction, probability, confidence);

            assertThat(prediction.id()).isEqualTo(1L);
            assertThat(prediction.symbol()).isEqualTo(symbol);
            assertThat(prediction.timestamp()).isEqualTo(timestamp);
            assertThat(prediction.direction()).isEqualTo(direction);
            assertThat(prediction.probability()).isEqualByComparingTo(probability);
            assertThat(prediction.confidence()).isEqualByComparingTo(confidence);
        }

        @Test
        @DisplayName("Should normalize probability to max 1")
        void shouldNormalizeProbabilityToMax1() {
            Symbol symbol = createTestSymbol();
            Prediction prediction = new Prediction(1L, symbol, Instant.now(), 
                PredictionDirection.UP, BigDecimal.valueOf(1.5), null);

            assertThat(prediction.probability()).isEqualByComparingTo(BigDecimal.ONE);
        }

        @Test
        @DisplayName("Should normalize probability to min 0")
        void shouldNormalizeProbabilityToMin0() {
            Symbol symbol = createTestSymbol();
            Prediction prediction = new Prediction(1L, symbol, Instant.now(), 
                PredictionDirection.UP, BigDecimal.valueOf(-0.5), null);

            assertThat(prediction.probability()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should normalize confidence to max 1")
        void shouldNormalizeConfidenceToMax1() {
            Symbol symbol = createTestSymbol();
            Prediction prediction = new Prediction(1L, symbol, Instant.now(), 
                PredictionDirection.UP, BigDecimal.valueOf(0.5), BigDecimal.valueOf(2.0));

            assertThat(prediction.confidence()).isEqualByComparingTo(BigDecimal.ONE);
        }

        @Test
        @DisplayName("Should normalize confidence to min 0")
        void shouldNormalizeConfidenceToMin0() {
            Symbol symbol = createTestSymbol();
            Prediction prediction = new Prediction(1L, symbol, Instant.now(), 
                PredictionDirection.UP, BigDecimal.valueOf(0.5), BigDecimal.valueOf(-1.0));

            assertThat(prediction.confidence()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should set default timestamp when null")
        void shouldSetDefaultTimestampWhenNull() {
            Symbol symbol = createTestSymbol();
            Instant before = Instant.now();
            Prediction prediction = new Prediction(1L, symbol, null, 
                PredictionDirection.UP, BigDecimal.valueOf(0.5), null);
            Instant after = Instant.now();

            assertThat(prediction.timestamp()).isAfterOrEqualTo(before);
            assertThat(prediction.timestamp()).isBeforeOrEqualTo(after);
        }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create prediction with basic parameters")
        void shouldCreatePredictionWithBasicParams() {
            Symbol symbol = createTestSymbol();

            Prediction prediction = Prediction.create(symbol, PredictionDirection.UP, BigDecimal.valueOf(0.7));

            assertThat(prediction.symbol()).isEqualTo(symbol);
            assertThat(prediction.direction()).isEqualTo(PredictionDirection.UP);
            assertThat(prediction.probability()).isEqualByComparingTo(BigDecimal.valueOf(0.7));
            assertThat(prediction.confidence()).isNull();
            assertThat(prediction.timestamp()).isNotNull();
        }

        @Test
        @DisplayName("Should create prediction with all parameters")
        void shouldCreatePredictionWithAllParams() {
            Symbol symbol = createTestSymbol();

            Prediction prediction = Prediction.create(symbol, PredictionDirection.DOWN, 
                BigDecimal.valueOf(0.6), BigDecimal.valueOf(0.8));

            assertThat(prediction.symbol()).isEqualTo(symbol);
            assertThat(prediction.direction()).isEqualTo(PredictionDirection.DOWN);
            assertThat(prediction.probability()).isEqualByComparingTo(BigDecimal.valueOf(0.6));
            assertThat(prediction.confidence()).isEqualByComparingTo(BigDecimal.valueOf(0.8));
        }

        @Test
        @DisplayName("Should normalize probability in factory method")
        void shouldNormalizeProbabilityInFactory() {
            Symbol symbol = createTestSymbol();

            Prediction prediction = Prediction.create(symbol, PredictionDirection.UP, BigDecimal.valueOf(1.5));

            assertThat(prediction.probability()).isEqualByComparingTo(BigDecimal.ONE);
        }
    }

    @Nested
    @DisplayName("Direction Tests")
    class DirectionTests {

        @Test
        @DisplayName("Should handle UP direction")
        void shouldHandleUpDirection() {
            Symbol symbol = createTestSymbol();
            Prediction prediction = Prediction.create(symbol, PredictionDirection.UP, BigDecimal.valueOf(0.8));

            assertThat(prediction.direction()).isEqualTo(PredictionDirection.UP);
        }

        @Test
        @DisplayName("Should handle DOWN direction")
        void shouldHandleDownDirection() {
            Symbol symbol = createTestSymbol();
            Prediction prediction = Prediction.create(symbol, PredictionDirection.DOWN, BigDecimal.valueOf(0.8));

            assertThat(prediction.direction()).isEqualTo(PredictionDirection.DOWN);
        }

        @Test
        @DisplayName("Should handle NEUTRAL direction")
        void shouldHandleNeutralDirection() {
            Symbol symbol = createTestSymbol();
            Prediction prediction = Prediction.create(symbol, PredictionDirection.NEUTRAL, BigDecimal.valueOf(0.5));

            assertThat(prediction.direction()).isEqualTo(PredictionDirection.NEUTRAL);
        }
    }
}

package com.mecho.domain.prediction;

import com.mecho.TestDataFactory;
import com.mecho.domain.indicators.*;
import com.mecho.domain.model.OHLCV;
import com.mecho.domain.model.PredictionDirection;
import com.mecho.domain.model.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WeightedPredictionCalculatorTest {

    private Symbol testSymbol;
    private List<OHLCV> uptrendPrices;
    private List<OHLCV> downtrendPrices;
    private List<OHLCV> flatPrices;

    @BeforeEach
    void setUp() {
        testSymbol = Symbol.builder().ticker("AAPL").build();
        uptrendPrices = TestDataFactory.createIncreasingPriceList(300, 100.0, 10.0);
        downtrendPrices = TestDataFactory.createDecreasingPriceList(300, 300.0, 10.0);
        flatPrices = TestDataFactory.createFlatPriceList(300, 100.0);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should use default weights when no map provided")
        void shouldUseDefaultWeightsWhenNoMapProvided() {
            WeightedPredictionCalculator calculator = new WeightedPredictionCalculator();

            assertThat(calculator.getWeight("RSI")).isEqualTo(0.20);
            assertThat(calculator.getWeight("MACD")).isEqualTo(0.25);
            assertThat(calculator.getWeight("BB")).isEqualTo(0.20);
            assertThat(calculator.getWeight("MACROSS")).isEqualTo(0.25);
            assertThat(calculator.getWeight("STOCH")).isEqualTo(0.10);
        }

        @Test
        @DisplayName("Should use provided weights")
        void shouldUseProvidedWeights() {
            Map<String, Double> customWeights = new HashMap<>();
            customWeights.put("RSI", 0.30);
            customWeights.put("MACD", 0.30);
            customWeights.put("BB", 0.20);
            customWeights.put("MACROSS", 0.10);
            customWeights.put("STOCH", 0.10);

            WeightedPredictionCalculator calculator = new WeightedPredictionCalculator(customWeights);

            assertThat(calculator.getWeight("RSI")).isEqualTo(0.30);
            assertThat(calculator.getWeight("MACD")).isEqualTo(0.30);
        }
    }

    @Nested
    @DisplayName("Calculate Method Tests")
    class CalculateTests {

        @Test
        @DisplayName("Should return neutral prediction for null prices")
        void shouldReturnNeutralForNullPrices() {
            WeightedPredictionCalculator calculator = new WeightedPredictionCalculator();
            List<Indicator> indicators = List.of(new RSIIndicator());

            Prediction prediction = calculator.calculate(testSymbol, null, indicators);

            assertThat(prediction.direction()).isEqualTo(PredictionDirection.NEUTRAL);
            assertThat(prediction.probability()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should return neutral prediction for empty prices")
        void shouldReturnNeutralForEmptyPrices() {
            WeightedPredictionCalculator calculator = new WeightedPredictionCalculator();
            List<Indicator> indicators = List.of(new RSIIndicator());

            Prediction prediction = calculator.calculate(testSymbol, List.of(), indicators);

            assertThat(prediction.direction()).isEqualTo(PredictionDirection.NEUTRAL);
            assertThat(prediction.probability()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should return neutral prediction for null indicators")
        void shouldReturnNeutralForNullIndicators() {
            WeightedPredictionCalculator calculator = new WeightedPredictionCalculator();

            Prediction prediction = calculator.calculate(testSymbol, uptrendPrices, null);

            assertThat(prediction.direction()).isEqualTo(PredictionDirection.NEUTRAL);
            assertThat(prediction.probability()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should return neutral prediction for empty indicators")
        void shouldReturnNeutralForEmptyIndicators() {
            WeightedPredictionCalculator calculator = new WeightedPredictionCalculator();

            Prediction prediction = calculator.calculate(testSymbol, uptrendPrices, List.of());

            assertThat(prediction.direction()).isEqualTo(PredictionDirection.NEUTRAL);
            assertThat(prediction.probability()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should return UP direction for uptrend")
        void shouldReturnUpDirectionForUptrend() {
            WeightedPredictionCalculator calculator = new WeightedPredictionCalculator();
            List<Indicator> indicators = List.of(
                new RSIIndicator(),
                new MACDIndicator(),
                new BollingerBandsIndicator(),
                new MovingAverageCrossoverIndicator(),
                new StochasticIndicator()
            );

            Prediction prediction = calculator.calculate(testSymbol, uptrendPrices, indicators);

            assertThat(prediction.direction()).isEqualTo(PredictionDirection.UP);
            assertThat(prediction.probability()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should return DOWN direction for downtrend")
        void shouldReturnDownDirectionForDowntrend() {
            WeightedPredictionCalculator calculator = new WeightedPredictionCalculator();
            List<Indicator> indicators = List.of(
                new RSIIndicator(),
                new MACDIndicator(),
                new BollingerBandsIndicator(),
                new MovingAverageCrossoverIndicator(),
                new StochasticIndicator()
            );

            Prediction prediction = calculator.calculate(testSymbol, downtrendPrices, indicators);

            assertThat(prediction.direction()).isEqualTo(PredictionDirection.DOWN);
            assertThat(prediction.probability()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should return NEUTRAL direction for flat prices")
        void shouldReturnNeutralForFlatPrices() {
            WeightedPredictionCalculator calculator = new WeightedPredictionCalculator();
            List<Indicator> indicators = List.of(
                new RSIIndicator(),
                new MACDIndicator(),
                new BollingerBandsIndicator(),
                new MovingAverageCrossoverIndicator(),
                new StochasticIndicator()
            );

            Prediction prediction = calculator.calculate(testSymbol, flatPrices, indicators);

            assertThat(prediction.direction()).isEqualTo(PredictionDirection.NEUTRAL);
        }
    }

    @Nested
    @DisplayName("Weight Modification Tests")
    class WeightModificationTests {

        @Test
        @DisplayName("Should set custom weight")
        void shouldSetCustomWeight() {
            WeightedPredictionCalculator calculator = new WeightedPredictionCalculator();

            calculator.setWeight("CUSTOM", 0.50);

            assertThat(calculator.getWeight("CUSTOM")).isEqualTo(0.50);
        }

        @Test
        @DisplayName("Should override existing weight")
        void shouldOverrideExistingWeight() {
            WeightedPredictionCalculator calculator = new WeightedPredictionCalculator();

            calculator.setWeight("RSI", 0.40);

            assertThat(calculator.getWeight("RSI")).isEqualTo(0.40);
        }

        @Test
        @DisplayName("Should return null for unknown weight")
        void shouldReturnNullForUnknownWeight() {
            WeightedPredictionCalculator calculator = new WeightedPredictionCalculator();

            assertThat(calculator.getWeight("UNKNOWN")).isNull();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle indicator with zero weight")
        void shouldHandleZeroWeightIndicator() {
            Map<String, Double> weights = new HashMap<>();
            weights.put("RSI", 0.0);
            weights.put("MACD", 0.5);
            weights.put("BB", 0.5);

            WeightedPredictionCalculator calculator = new WeightedPredictionCalculator(weights);
            List<Indicator> indicators = List.of(new RSIIndicator(), new MACDIndicator());

            Prediction prediction = calculator.calculate(testSymbol, uptrendPrices, indicators);

            assertThat(prediction).isNotNull();
            assertThat(prediction.direction()).isEqualTo(PredictionDirection.UP);
        }

        @Test
        @DisplayName("Should handle all indicators with zero weight")
        void shouldHandleAllZeroWeights() {
            Map<String, Double> weights = new HashMap<>();
            weights.put("RSI", 0.0);
            weights.put("MACD", 0.0);

            WeightedPredictionCalculator calculator = new WeightedPredictionCalculator(weights);
            List<Indicator> indicators = List.of(new RSIIndicator(), new MACDIndicator());

            Prediction prediction = calculator.calculate(testSymbol, uptrendPrices, indicators);

            assertThat(prediction.direction()).isEqualTo(PredictionDirection.NEUTRAL);
            assertThat(prediction.probability()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}

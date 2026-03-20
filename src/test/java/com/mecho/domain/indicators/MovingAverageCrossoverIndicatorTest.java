package com.mecho.domain.indicators;

import com.mecho.TestDataFactory;
import com.mecho.domain.model.OHLCV;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MovingAverageCrossoverIndicatorTest {

    private MovingAverageCrossoverIndicator indicator;

    @BeforeEach
    void setUp() {
        indicator = new MovingAverageCrossoverIndicator();
    }

    @Test
    @DisplayName("Should return correct indicator code")
    void shouldReturnCorrectCode() {
        assertThat(indicator.getCode()).isEqualTo("MACROSS");
    }

    @Nested
    @DisplayName("Calculate Method Tests")
    class CalculateTests {

        @Test
        @DisplayName("Should return 0 for null input")
        void shouldReturnZeroForNullInput() {
            double result = indicator.calculate(null);
            assertThat(result).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0 for empty list")
        void shouldReturnZeroForEmptyList() {
            double result = indicator.calculate(Collections.emptyList());
            assertThat(result).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0 for insufficient data")
        void shouldReturnZeroForInsufficientData() {
            List<OHLCV> prices = TestDataFactory.createPriceList(100, 101, 102, 103, 104, 105, 106, 107, 108, 109);
            double result = indicator.calculate(prices);
            assertThat(result).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return value for exactly minimum data")
        void shouldReturnValueForMinimumData() {
            List<OHLCV> prices = TestDataFactory.createIncreasingPriceList(201, 100.0, 0.5);
            double result = indicator.calculate(prices);
            assertThat(result).isNotEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 1.0 for sustained bullish trend")
        void shouldReturnSustainedBullish() {
            List<OHLCV> result = new java.util.ArrayList<>();
            for (int i = 0; i < 400; i++) {
                double price;
                if (i < 250) {
                    price = 100 - i * 0.1;
                } else {
                    price = 75 + (i - 250) * 0.5;
                }
                result.add(TestDataFactory.createOHLCV(price, price, price, price, 1000));
            }
            double value = indicator.calculate(result);
            assertThat(value).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should return -1.0 for sustained bearish trend")
        void shouldReturnSustainedBearishTrendV2() {
            List<OHLCV> result = new java.util.ArrayList<>();
            for (int i = 0; i < 400; i++) {
                double price;
                if (i < 250) {
                    price = 100 + i * 0.1;
                } else {
                    price = 125 - (i - 250) * 0.5;
                }
                result.add(TestDataFactory.createOHLCV(price, price, price, price, 1000));
            }
            double value = indicator.calculate(result);
            assertThat(value).isEqualTo(-1.0);
        }

        @Test
        @DisplayName("Should return 1.0 for sustained bullish trend")
        void shouldReturnSustainedBullishTrend() {
            List<OHLCV> prices = TestDataFactory.createIncreasingPriceList(250, 100.0, 0.5);
            double result = indicator.calculate(prices);
            assertThat(result).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should return -1.0 for sustained bearish trend")
        void shouldReturnSustainedBearishTrend() {
            List<OHLCV> prices = TestDataFactory.createDecreasingPriceList(250, 200.0, 0.5);
            double result = indicator.calculate(prices);
            assertThat(result).isEqualTo(-1.0);
        }

        @Test
        @DisplayName("Should return 0.0 for flat trend")
        void shouldReturnZeroForFlatTrend() {
            List<OHLCV> prices = createTrulyFlatPriceList(250, 100.0);
            double result = indicator.calculate(prices);
            assertThat(result).isEqualTo(0.0);
        }
        
        private static List<OHLCV> createTrulyFlatPriceList(int count, double price) {
            List<OHLCV> result = new java.util.ArrayList<>();
            for (int i = 0; i < count; i++) {
                result.add(TestDataFactory.createOHLCV(price, price, price, price, 1000));
            }
            return result;
        }
    }

    @Nested
    @DisplayName("Signal Generation Tests")
    class SignalTests {

        @Test
        @DisplayName("Should return BUY for positive crossover signal")
        void shouldReturnBuyForPositiveCrossover() {
            Signal signal = indicator.calculateSignal(2.0);
            assertThat(signal).isEqualTo(Signal.BUY);
        }

        @Test
        @DisplayName("Should return BUY for sustained bullish signal")
        void shouldReturnBuyForSustainedBullish() {
            Signal signal = indicator.calculateSignal(1.0);
            assertThat(signal).isEqualTo(Signal.BUY);
        }

        @Test
        @DisplayName("Should return SELL for negative crossover signal")
        void shouldReturnSellForNegativeCrossover() {
            Signal signal = indicator.calculateSignal(-2.0);
            assertThat(signal).isEqualTo(Signal.SELL);
        }

        @Test
        @DisplayName("Should return SELL for sustained bearish signal")
        void shouldReturnSellForSustainedBearish() {
            Signal signal = indicator.calculateSignal(-1.0);
            assertThat(signal).isEqualTo(Signal.SELL);
        }

        @Test
        @DisplayName("Should return NEUTRAL for no trend")
        void shouldReturnNeutralForNoTrend() {
            Signal signal = indicator.calculateSignal(0.0);
            assertThat(signal).isEqualTo(Signal.NEUTRAL);
        }

        @Test
        @DisplayName("Should return NEUTRAL for small positive value")
        void shouldReturnNeutralForSmallPositive() {
            Signal signal = indicator.calculateSignal(0.5);
            assertThat(signal).isEqualTo(Signal.NEUTRAL);
        }

        @Test
        @DisplayName("Should return NEUTRAL for small negative value")
        void shouldReturnNeutralForSmallNegative() {
            Signal signal = indicator.calculateSignal(-0.5);
            assertThat(signal).isEqualTo(Signal.NEUTRAL);
        }

        @Test
        @DisplayName("Should handle boundary at 1.0")
        void shouldHandleBoundaryAt1() {
            assertThat(indicator.calculateSignal(0.99)).isEqualTo(Signal.NEUTRAL);
            assertThat(indicator.calculateSignal(1.0)).isEqualTo(Signal.BUY);
        }

        @Test
        @DisplayName("Should handle boundary at -1.0")
        void shouldHandleBoundaryAtMinus1() {
            assertThat(indicator.calculateSignal(-0.99)).isEqualTo(Signal.NEUTRAL);
            assertThat(indicator.calculateSignal(-1.0)).isEqualTo(Signal.SELL);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle exactly 201 data points")
        void shouldHandleExactly201Points() {
            List<OHLCV> prices = TestDataFactory.createIncreasingPriceList(201, 100.0, 0.5);
            double result = indicator.calculate(prices);
            assertThat(result).isNotEqualTo(0.0);
        }

        @Test
        @DisplayName("Should handle exactly 200 data points")
        void shouldHandleExactly200Points() {
            List<OHLCV> prices = TestDataFactory.createIncreasingPriceList(200, 100.0, 0.5);
            double result = indicator.calculate(prices);
            assertThat(result).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should handle volatile data")
        void shouldHandleVolatileData() {
            List<OHLCV> prices = TestDataFactory.createVolatilePriceList(250, 100.0);
            double result = indicator.calculate(prices);
            assertThat(result).isBetween(-2.0, 2.0);
        }
    }
}

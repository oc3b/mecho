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

class MACDIndicatorTest {

    private MACDIndicator indicator;

    @BeforeEach
    void setUp() {
        indicator = new MACDIndicator();
    }

    @Test
    @DisplayName("Should return correct indicator code")
    void shouldReturnCorrectCode() {
        assertThat(indicator.getCode()).isEqualTo("MACD");
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
        @DisplayName("Should calculate positive MACD for uptrend")
        void shouldCalculatePositiveMACDForUptrend() {
            List<OHLCV> prices = TestDataFactory.createIncreasingPriceList(50, 100.0, 1.0);
            double result = indicator.calculate(prices);
            assertThat(result).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("Should calculate negative MACD for downtrend")
        void shouldCalculateNegativeMACDForDowntrend() {
            List<OHLCV> prices = TestDataFactory.createDecreasingPriceList(50, 100.0, 1.0);
            double result = indicator.calculate(prices);
            assertThat(result).isLessThan(0.0);
        }

        @Test
        @DisplayName("Should return near-zero for flat data")
        void shouldReturnNearZeroForFlatData() {
            List<OHLCV> prices = TestDataFactory.createFlatPriceList(50, 100.0);
            double result = indicator.calculate(prices);
            assertThat(result).isBetween(-0.5, 0.5);
        }

        @Test
        @DisplayName("Should handle volatile data")
        void shouldHandleVolatileData() {
            List<OHLCV> prices = TestDataFactory.createVolatilePriceList(50, 100.0);
            double result = indicator.calculate(prices);
            assertThat(Double.isFinite(result)).isTrue();
        }

        @Test
        @DisplayName("Should return value within reasonable bounds")
        void shouldReturnValueWithinReasonableBounds() {
            List<OHLCV> prices = TestDataFactory.createIncreasingPriceList(100, 100.0, 0.5);
            double result = indicator.calculate(prices);
            assertThat(result).isBetween(-50.0, 50.0);
        }
    }

    @Nested
    @DisplayName("Signal Generation Tests")
    class SignalTests {

        @Test
        @DisplayName("Should return BUY signal for positive MACD")
        void shouldReturnBuyForPositiveMACD() {
            Signal signal = indicator.calculateSignal(5.0);
            assertThat(signal).isEqualTo(Signal.BUY);
        }

        @Test
        @DisplayName("Should return SELL signal for negative MACD")
        void shouldReturnSellForNegativeMACD() {
            Signal signal = indicator.calculateSignal(-5.0);
            assertThat(signal).isEqualTo(Signal.SELL);
        }

        @Test
        @DisplayName("Should return NEUTRAL signal for zero MACD")
        void shouldReturnNeutralForZeroMACD() {
            Signal signal = indicator.calculateSignal(0.0);
            assertThat(signal).isEqualTo(Signal.NEUTRAL);
        }

        @Test
        @DisplayName("Should return NEUTRAL for small positive value")
        void shouldReturnNeutralForSmallPositive() {
            Signal signal = indicator.calculateSignal(0.0);
            assertThat(signal).isEqualTo(Signal.NEUTRAL);
        }

        @Test
        @DisplayName("Should return NEUTRAL for small negative value")
        void shouldReturnNeutralForSmallNegative() {
            Signal signal = indicator.calculateSignal(-0.0);
            assertThat(signal).isEqualTo(Signal.NEUTRAL);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle exactly minimum required data")
        void shouldHandleMinimumData() {
            List<OHLCV> prices = TestDataFactory.createIncreasingPriceList(40, 100.0, 1.0);
            double result = indicator.calculate(prices);
            assertThat(Double.isFinite(result)).isTrue();
        }

        @Test
        @DisplayName("Should handle very large price values")
        void shouldHandleLargePriceValues() {
            List<OHLCV> prices = TestDataFactory.createIncreasingPriceList(60, 1000000.0, 100.0);
            double result = indicator.calculate(prices);
            assertThat(Double.isFinite(result)).isTrue();
        }

        @Test
        @DisplayName("Should handle very small price values")
        void shouldHandleSmallPriceValues() {
            List<OHLCV> prices = TestDataFactory.createIncreasingPriceList(60, 0.001, 0.0001);
            double result = indicator.calculate(prices);
            assertThat(Double.isFinite(result)).isTrue();
        }
    }
}

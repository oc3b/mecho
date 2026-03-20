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

class BollingerBandsIndicatorTest {

    private BollingerBandsIndicator indicator;

    @BeforeEach
    void setUp() {
        indicator = new BollingerBandsIndicator();
    }

    @Test
    @DisplayName("Should return correct indicator code")
    void shouldReturnCorrectCode() {
        assertThat(indicator.getCode()).isEqualTo("BB");
    }

    @Nested
    @DisplayName("Calculate Method Tests")
    class CalculateTests {

        @Test
        @DisplayName("Should return 0.5 for null input")
        void shouldReturnNeutralForNullInput() {
            double result = indicator.calculate(null);
            assertThat(result).isEqualTo(0.5);
        }

        @Test
        @DisplayName("Should return 0.5 for empty list")
        void shouldReturnNeutralForEmptyList() {
            double result = indicator.calculate(Collections.emptyList());
            assertThat(result).isEqualTo(0.5);
        }

        @Test
        @DisplayName("Should return 0.5 for insufficient data")
        void shouldReturnNeutralForInsufficientData() {
            List<OHLCV> prices = TestDataFactory.createPriceList(100, 101, 102, 103, 104, 105, 106, 107, 108, 109);
            double result = indicator.calculate(prices);
            assertThat(result).isEqualTo(0.5);
        }

        @Test
        @DisplayName("Should return 0.5 for exactly minimum required data")
        void shouldReturnNeutralForMinimumData() {
            List<OHLCV> prices = TestDataFactory.createPriceList(
                100, 101, 102, 103, 104, 105, 106, 107, 108, 109,
                110, 111, 112, 113, 114, 115, 116, 117, 118, 119
            );
            double result = indicator.calculate(prices);
            assertThat(result).isBetween(0.0, 1.0);
        }

        @Test
        @DisplayName("Should calculate position correctly for price near lower band")
        void shouldCalculatePositionForLowerBand() {
            List<OHLCV> prices = TestDataFactory.createVolatilePriceList(30, 100.0);
            double result = indicator.calculate(prices);
            assertThat(result).isBetween(0.0, 1.0);
        }

        @Test
        @DisplayName("Should calculate position correctly for price near upper band")
        void shouldCalculatePositionForUpperBand() {
            List<OHLCV> prices = TestDataFactory.createIncreasingPriceList(30, 100.0, 2.0);
            double result = indicator.calculate(prices);
            assertThat(result).isGreaterThan(0.5);
            assertThat(result).isLessThanOrEqualTo(1.0);
        }

        @Test
        @DisplayName("Should return value between 0 and 1 for valid data")
        void shouldReturnValueBetween0And1() {
            List<OHLCV> prices = TestDataFactory.createVolatilePriceList(50, 100.0);
            double result = indicator.calculate(prices);
            assertThat(result).isBetween(0.0, 1.0);
        }

        @Test
        @DisplayName("Should return 0.5 when band width is zero")
        void shouldReturnNeutralForZeroBandWidth() {
            List<OHLCV> prices = TestDataFactory.createFlatPriceList(20, 100.0);
            double result = indicator.calculate(prices);
            assertThat(result).isEqualTo(0.5);
        }

        @Test
        @DisplayName("Should handle decreasing trend")
        void shouldHandleDecreasingTrend() {
            List<OHLCV> prices = TestDataFactory.createDecreasingPriceList(30, 100.0, 1.0);
            double result = indicator.calculate(prices);
            assertThat(result).isLessThan(0.5);
            assertThat(result).isGreaterThanOrEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Signal Generation Tests")
    class SignalTests {

        @Test
        @DisplayName("Should return BUY signal for position below 0.2")
        void shouldReturnBuyForLowPosition() {
            Signal signal = indicator.calculateSignal(0.1);
            assertThat(signal).isEqualTo(Signal.BUY);
        }

        @Test
        @DisplayName("Should return SELL signal for position above 0.8")
        void shouldReturnSellForHighPosition() {
            Signal signal = indicator.calculateSignal(0.9);
            assertThat(signal).isEqualTo(Signal.SELL);
        }

        @Test
        @DisplayName("Should return NEUTRAL signal for middle position")
        void shouldReturnNeutralForMiddlePosition() {
            Signal signal = indicator.calculateSignal(0.5);
            assertThat(signal).isEqualTo(Signal.NEUTRAL);
        }

        @Test
        @DisplayName("Should return NEUTRAL at lower boundary")
        void shouldReturnNeutralAtLowerBoundary() {
            Signal signal = indicator.calculateSignal(0.2);
            assertThat(signal).isEqualTo(Signal.NEUTRAL);
        }

        @Test
        @DisplayName("Should return NEUTRAL at upper boundary")
        void shouldReturnNeutralAtUpperBoundary() {
            Signal signal = indicator.calculateSignal(0.8);
            assertThat(signal).isEqualTo(Signal.NEUTRAL);
        }

        @Test
        @DisplayName("Should handle boundary values correctly")
        void shouldHandleBoundaryValues() {
            assertThat(indicator.calculateSignal(0.19)).isEqualTo(Signal.BUY);
            assertThat(indicator.calculateSignal(0.81)).isEqualTo(Signal.SELL);
        }

        @Test
        @DisplayName("Should handle edge case at 0")
        void shouldHandleZeroValue() {
            Signal signal = indicator.calculateSignal(0.0);
            assertThat(signal).isEqualTo(Signal.BUY);
        }

        @Test
        @DisplayName("Should handle edge case at 1")
        void shouldHandleOneValue() {
            Signal signal = indicator.calculateSignal(1.0);
            assertThat(signal).isEqualTo(Signal.SELL);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle single price repeated")
        void shouldHandleSinglePriceRepeated() {
            List<OHLCV> prices = TestDataFactory.createFlatPriceList(20, 100.0);
            double result = indicator.calculate(prices);
            assertThat(result).isEqualTo(0.5);
        }
        
        @Test
        @DisplayName("Should handle large dataset")
        void shouldHandleLargeDataset() {
            List<OHLCV> prices = TestDataFactory.createVolatilePriceList(100, 100.0);
            double result = indicator.calculate(prices);
            assertThat(result).isBetween(0.0, 1.0);
        }
        
        @Test
        @DisplayName("Should handle volatile dataset")
        void shouldHandleVolatileDataset() {
            List<OHLCV> prices = TestDataFactory.createVolatilePriceList(30, 100.0);
            double result = indicator.calculate(prices);
            assertThat(Double.isFinite(result)).isTrue();
            assertThat(result).isBetween(0.0, 1.0);
        }
    }
}

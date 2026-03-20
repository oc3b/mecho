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

class StochasticIndicatorTest {

    private StochasticIndicator indicator;

    @BeforeEach
    void setUp() {
        indicator = new StochasticIndicator();
    }

    @Test
    @DisplayName("Should return correct indicator code")
    void shouldReturnCorrectCode() {
        assertThat(indicator.getCode()).isEqualTo("STOCH");
    }

    @Nested
    @DisplayName("Calculate Method Tests")
    class CalculateTests {

        @Test
        @DisplayName("Should return 50 for null input")
        void shouldReturn50ForNullInput() {
            double result = indicator.calculate(null);
            assertThat(result).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Should return 50 for empty list")
        void shouldReturn50ForEmptyList() {
            double result = indicator.calculate(Collections.emptyList());
            assertThat(result).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Should return 50 for insufficient data")
        void shouldReturn50ForInsufficientData() {
            List<OHLCV> prices = TestDataFactory.createPriceList(100, 101, 102, 103, 104, 105, 106, 107, 108, 109);
            double result = indicator.calculate(prices);
            assertThat(result).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Should return 50 for exactly minimum data")
        void shouldReturn50ForMinimumData() {
            List<OHLCV> prices = TestDataFactory.createPriceList(
                100, 101, 102, 103, 104, 105, 106, 107, 108, 109,
                110, 111, 112, 113, 114, 115
            );
            double result = indicator.calculate(prices);
            assertThat(result).isBetween(0.0, 100.0);
        }

        @Test
        @DisplayName("Should return value for sufficient data")
        void shouldReturnValueForSufficientData() {
            List<OHLCV> prices = TestDataFactory.createPriceList(
                100, 101, 102, 103, 104, 105, 106, 107, 108, 109,
                110, 111, 112, 113, 114, 115, 116, 117
            );
            double result = indicator.calculate(prices);
            assertThat(result).isBetween(0.0, 100.0);
        }

        @Test
        @DisplayName("Should return 100 when price equals high")
        void shouldReturn100WhenPriceEqualsHigh() {
            List<OHLCV> result = new java.util.ArrayList<>();
            for (int i = 0; i < 16; i++) {
                result.add(TestDataFactory.createOHLCV(100 + i, 115, 100 + i, 115, 1000));
            }
            double stoch = indicator.calculate(result);
            assertThat(stoch).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should return 0 when price equals low")
        void shouldReturn0WhenPriceEqualsLow() {
            List<OHLCV> result = new java.util.ArrayList<>();
            for (int i = 0; i < 16; i++) {
                result.add(TestDataFactory.createOHLCV(100 + i, 100 + i, 85, 85, 1000));
            }
            double stoch = indicator.calculate(result);
            assertThat(stoch).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 50 when price is in middle of range")
        void shouldReturn50WhenPriceInMiddle() {
            List<OHLCV> result = new java.util.ArrayList<>();
            for (int i = 0; i < 16; i++) {
                result.add(TestDataFactory.createOHLCV(100, 100, 100, 100, 1000));
            }
            double stoch = indicator.calculate(result);
            assertThat(stoch).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Should return value between 0 and 100")
        void shouldReturnValueBetween0And100() {
            List<OHLCV> prices = TestDataFactory.createStochasticTestData();
            double result = indicator.calculate(prices);
            assertThat(result).isBetween(0.0, 100.0);
        }

        @Test
        @DisplayName("Should handle decreasing trend")
        void shouldHandleDecreasingTrend() {
            List<OHLCV> prices = TestDataFactory.createDecreasingPriceList(30, 100.0, 1.0);
            double result = indicator.calculate(prices);
            assertThat(result).isBetween(0.0, 100.0);
        }

        @Test
        @DisplayName("Should handle increasing trend")
        void shouldHandleIncreasingTrend() {
            List<OHLCV> prices = TestDataFactory.createIncreasingPriceList(30, 100.0, 1.0);
            double result = indicator.calculate(prices);
            assertThat(result).isBetween(0.0, 100.0);
        }
    }

    @Nested
    @DisplayName("Signal Generation Tests")
    class SignalTests {

        @Test
        @DisplayName("Should return BUY signal for oversold stochastic")
        void shouldReturnBuyForOversold() {
            Signal signal = indicator.calculateSignal(15.0);
            assertThat(signal).isEqualTo(Signal.BUY);
        }

        @Test
        @DisplayName("Should return SELL signal for overbought stochastic")
        void shouldReturnSellForOverbought() {
            Signal signal = indicator.calculateSignal(85.0);
            assertThat(signal).isEqualTo(Signal.SELL);
        }

        @Test
        @DisplayName("Should return NEUTRAL signal for neutral stochastic")
        void shouldReturnNeutralForNeutral() {
            Signal signal = indicator.calculateSignal(50.0);
            assertThat(signal).isEqualTo(Signal.NEUTRAL);
        }

        @Test
        @DisplayName("Should return NEUTRAL at oversold boundary")
        void shouldReturnNeutralAtOversoldBoundary() {
            Signal signal = indicator.calculateSignal(20.0);
            assertThat(signal).isEqualTo(Signal.NEUTRAL);
        }

        @Test
        @DisplayName("Should return NEUTRAL at overbought boundary")
        void shouldReturnNeutralAtOverboughtBoundary() {
            Signal signal = indicator.calculateSignal(80.0);
            assertThat(signal).isEqualTo(Signal.NEUTRAL);
        }

        @Test
        @DisplayName("Should handle boundary values correctly")
        void shouldHandleBoundaryValues() {
            assertThat(indicator.calculateSignal(19.9)).isEqualTo(Signal.BUY);
            assertThat(indicator.calculateSignal(80.1)).isEqualTo(Signal.SELL);
        }

        @Test
        @DisplayName("Should handle extreme values")
        void shouldHandleExtremeValues() {
            assertThat(indicator.calculateSignal(0.0)).isEqualTo(Signal.BUY);
            assertThat(indicator.calculateSignal(100.0)).isEqualTo(Signal.SELL);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle exactly 16 data points")
        void shouldHandleExactly16Points() {
            List<OHLCV> prices = TestDataFactory.createPriceList(
                100, 101, 102, 103, 104, 105, 106, 107, 108, 109,
                110, 111, 112, 113, 114, 115
            );
            double result = indicator.calculate(prices);
            assertThat(result).isBetween(0.0, 100.0);
        }

        @Test
        @DisplayName("Should handle high volatility")
        void shouldHandleHighVolatility() {
            List<OHLCV> prices = TestDataFactory.createVolatilePriceList(20, 100.0);
            double result = indicator.calculate(prices);
            assertThat(result).isBetween(0.0, 100.0);
        }

        @Test
        @DisplayName("Should handle flat prices with variation")
        void shouldHandleFlatPricesWithVariation() {
            List<OHLCV> result = new java.util.ArrayList<>();
            for (int i = 0; i < 20; i++) {
                result.add(TestDataFactory.createOHLCV(100, 102, 98, 100 + (i % 3), 1000));
            }
            double stoch = indicator.calculate(result);
            assertThat(stoch).isBetween(0.0, 100.0);
        }
    }
}

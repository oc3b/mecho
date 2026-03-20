package com.mecho.domain.indicators;

import com.mecho.TestDataFactory;
import com.mecho.domain.model.OHLCV;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RSIIndicatorTest {

    private RSIIndicator indicator;

    @BeforeEach
    void setUp() {
        indicator = new RSIIndicator();
    }

    @Test
    @DisplayName("Should return correct indicator code")
    void shouldReturnCorrectCode() {
        assertThat(indicator.getCode()).isEqualTo("RSI");
    }

    @Nested
    @DisplayName("Calculate Method Tests")
    class CalculateTests {

        @Test
        @DisplayName("Should return neutral value for null input")
        void shouldReturnNeutralForNullInput() {
            double result = indicator.calculate(null);
            assertThat(result).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Should return neutral value for empty list")
        void shouldReturnNeutralForEmptyList() {
            double result = indicator.calculate(Collections.emptyList());
            assertThat(result).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Should return neutral value for insufficient data")
        void shouldReturnNeutralForInsufficientData() {
            List<OHLCV> prices = TestDataFactory.createPriceList(100, 101, 102, 103);
            double result = indicator.calculate(prices);
            assertThat(result).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Should return neutral value when period is exactly minimum")
        void shouldReturnNeutralForMinimumPeriod() {
            List<OHLCV> prices = TestDataFactory.createPriceList(
                100, 101, 102, 103, 104, 105, 106, 107, 108, 109,
                110, 111, 112, 113, 114
            );
            double result = indicator.calculate(prices);
            assertThat(result).isBetween(0.0, 100.0);
        }

        @Test
        @DisplayName("Should calculate RSI correctly for trending up data")
        void shouldCalculateRSIForUpTrend() {
            List<OHLCV> prices = TestDataFactory.createIncreasingPriceList(20, 100.0, 1.0);
            double result = indicator.calculate(prices);
            assertThat(result).isGreaterThan(50.0);
            assertThat(result).isLessThanOrEqualTo(100.0);
        }

        @Test
        @DisplayName("Should calculate RSI correctly for trending down data")
        void shouldCalculateRSIForDownTrend() {
            List<OHLCV> prices = TestDataFactory.createDecreasingPriceList(20, 100.0, 1.0);
            double result = indicator.calculate(prices);
            assertThat(result).isLessThan(50.0);
            assertThat(result).isGreaterThanOrEqualTo(0.0);
        }

        @Test
        @DisplayName("Should calculate RSI correctly for flat data")
        void shouldCalculateRSIForFlatData() {
            List<OHLCV> prices = TestDataFactory.createFlatPriceList(20, 100.0);
            double result = indicator.calculate(prices);
            assertThat(result).isBetween(0.0, 100.0);
        }

        @Test
        @DisplayName("Should return 100 when only gains occur")
        void shouldReturn100ForOnlyGains() {
            List<OHLCV> result = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                result.add(TestDataFactory.createOHLCV(100 + i, 101 + i, 99 + i, 101 + i, 1000));
            }
            double rsi = indicator.calculate(result);
            assertThat(rsi).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should return 0 when only losses occur")
        void shouldReturnNear0ForOnlyLosses() {
            List<OHLCV> result = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                result.add(TestDataFactory.createOHLCV(101 - i, 100 - i, 102 - i, 99 - i, 1000));
            }
            double rsi = indicator.calculate(result);
            assertThat(rsi).isLessThan(10.0);
        }
    }

    @Nested
    @DisplayName("Signal Generation Tests")
    class SignalTests {

        @Test
        @DisplayName("Should return BUY signal for oversold RSI")
        void shouldReturnBuyForOversold() {
            Signal signal = indicator.calculateSignal(25.0);
            assertThat(signal).isEqualTo(Signal.BUY);
        }

        @Test
        @DisplayName("Should return SELL signal for overbought RSI")
        void shouldReturnSellForOverbought() {
            Signal signal = indicator.calculateSignal(85.0);
            assertThat(signal).isEqualTo(Signal.SELL);
        }

        @Test
        @DisplayName("Should return NEUTRAL signal for neutral RSI")
        void shouldReturnNeutralForNeutral() {
            Signal signal = indicator.calculateSignal(50.0);
            assertThat(signal).isEqualTo(Signal.NEUTRAL);
        }

        @Test
        @DisplayName("Should return NEUTRAL signal at oversold boundary")
        void shouldReturnNeutralAtOversoldBoundary() {
            Signal signal = indicator.calculateSignal(30.0);
            assertThat(signal).isEqualTo(Signal.NEUTRAL);
        }

        @Test
        @DisplayName("Should return NEUTRAL signal at overbought boundary")
        void shouldReturnNeutralAtOverboughtBoundary() {
            Signal signal = indicator.calculateSignal(70.0);
            assertThat(signal).isEqualTo(Signal.NEUTRAL);
        }

        @Test
        @DisplayName("Should handle boundary values correctly")
        void shouldHandleBoundaryValues() {
            assertThat(indicator.calculateSignal(29.9)).isEqualTo(Signal.BUY);
            assertThat(indicator.calculateSignal(70.1)).isEqualTo(Signal.SELL);
        }
    }
}

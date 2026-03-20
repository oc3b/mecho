package com.mecho.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OHLCVTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create OHLCV with all values")
        void shouldCreateOHLCVWithAllValues() {
            Instant timestamp = Instant.now();
            BigDecimal open = BigDecimal.valueOf(100);
            BigDecimal high = BigDecimal.valueOf(105);
            BigDecimal low = BigDecimal.valueOf(99);
            BigDecimal close = BigDecimal.valueOf(103);
            BigDecimal volume = BigDecimal.valueOf(1000000);

            OHLCV ohlcv = new OHLCV(open, high, low, close, volume, timestamp);

            assertThat(ohlcv.open()).isEqualTo(open);
            assertThat(ohlcv.high()).isEqualTo(high);
            assertThat(ohlcv.low()).isEqualTo(low);
            assertThat(ohlcv.close()).isEqualTo(close);
            assertThat(ohlcv.volume()).isEqualTo(volume);
            assertThat(ohlcv.timestamp()).isEqualTo(timestamp);
        }

        @Test
        @DisplayName("Should set defaults for null values")
        void shouldSetDefaultsForNullValues() {
            OHLCV ohlcv = new OHLCV(null, null, null, null, null, null);

            assertThat(ohlcv.open()).isEqualTo(BigDecimal.ZERO);
            assertThat(ohlcv.high()).isEqualTo(BigDecimal.ZERO);
            assertThat(ohlcv.low()).isEqualTo(BigDecimal.ZERO);
            assertThat(ohlcv.close()).isEqualTo(BigDecimal.ZERO);
            assertThat(ohlcv.volume()).isEqualTo(BigDecimal.ZERO);
            assertThat(ohlcv.timestamp()).isNotNull();
        }

        @Test
        @DisplayName("Should preserve zero values")
        void shouldPreserveZeroValues() {
            OHLCV ohlcv = new OHLCV(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 
                                    BigDecimal.ZERO, BigDecimal.ZERO, Instant.now());

            assertThat(ohlcv.open()).isEqualTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Helper Method Tests")
    class HelperMethodTests {

        @Test
        @DisplayName("Should calculate typical price correctly")
        void shouldCalculateTypicalPrice() {
            OHLCV ohlcv = new OHLCV(
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(110),
                BigDecimal.valueOf(90),
                BigDecimal.valueOf(105),
                BigDecimal.valueOf(1000),
                Instant.now()
            );

            BigDecimal typicalPrice = ohlcv.getTypicalPrice();
            assertThat(typicalPrice).isEqualByComparingTo("101.66666667");
        }

        @Test
        @DisplayName("Should calculate price range correctly")
        void shouldCalculatePriceRange() {
            OHLCV ohlcv = new OHLCV(
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(110),
                BigDecimal.valueOf(90),
                BigDecimal.valueOf(105),
                BigDecimal.valueOf(1000),
                Instant.now()
            );

            BigDecimal priceRange = ohlcv.getPriceRange();
            assertThat(priceRange).isEqualByComparingTo(BigDecimal.valueOf(20));
        }

        @Test
        @DisplayName("Should handle zero range for typical price")
        void shouldHandleZeroRangeForTypicalPrice() {
            OHLCV ohlcv = new OHLCV(
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(1000),
                Instant.now()
            );

            BigDecimal typicalPrice = ohlcv.getTypicalPrice();
            assertThat(typicalPrice).isEqualByComparingTo(BigDecimal.valueOf(100));
        }

        @Test
        @DisplayName("Should handle zero range")
        void shouldHandleZeroRange() {
            OHLCV ohlcv = new OHLCV(
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(1000),
                Instant.now()
            );

            BigDecimal priceRange = ohlcv.getPriceRange();
            assertThat(priceRange).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle large decimal values")
        void shouldHandleLargeDecimalValues() {
            OHLCV ohlcv = new OHLCV(
                BigDecimal.valueOf(1_000_000_000.123456789),
                BigDecimal.valueOf(1_000_000_005.123456789),
                BigDecimal.valueOf(999_999_990.123456789),
                BigDecimal.valueOf(1_000_000_002.123456789),
                BigDecimal.valueOf(Long.MAX_VALUE),
                Instant.now()
            );

            assertThat(ohlcv.getTypicalPrice()).isNotNull();
            assertThat(ohlcv.getPriceRange()).isNotNull();
        }

        @Test
        @DisplayName("Should handle small decimal values")
        void shouldHandleSmallDecimalValues() {
            OHLCV ohlcv = new OHLCV(
                BigDecimal.valueOf(0.00000001),
                BigDecimal.valueOf(0.00000002),
                BigDecimal.valueOf(0.000000005),
                BigDecimal.valueOf(0.000000015),
                BigDecimal.valueOf(1),
                Instant.now()
            );

            assertThat(ohlcv.getTypicalPrice()).isNotNull();
            assertThat(ohlcv.getPriceRange()).isNotNull();
        }
    }
}

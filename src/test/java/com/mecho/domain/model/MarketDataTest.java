package com.mecho.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketDataTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create MarketData with all values")
        void shouldCreateMarketDataWithAllValues() {
            Symbol symbol = Symbol.builder().ticker("AAPL").build();
            Instant timestamp = Instant.now();
            BigDecimal open = BigDecimal.valueOf(100);
            BigDecimal high = BigDecimal.valueOf(105);
            BigDecimal low = BigDecimal.valueOf(99);
            BigDecimal close = BigDecimal.valueOf(103);
            BigDecimal volume = BigDecimal.valueOf(1000000);
            String dataSource = "ALPHA_VANTAGE";

            MarketData marketData = new MarketData(1L, symbol, timestamp, open, high, low, close, volume, dataSource);

            assertThat(marketData.id()).isEqualTo(1L);
            assertThat(marketData.symbol()).isEqualTo(symbol);
            assertThat(marketData.timestamp()).isEqualTo(timestamp);
            assertThat(marketData.openPrice()).isEqualTo(open);
            assertThat(marketData.highPrice()).isEqualTo(high);
            assertThat(marketData.lowPrice()).isEqualTo(low);
            assertThat(marketData.closePrice()).isEqualTo(close);
            assertThat(marketData.volume()).isEqualTo(volume);
            assertThat(marketData.dataSource()).isEqualTo(dataSource);
        }

        @Test
        @DisplayName("Should set default dataSource for null")
        void shouldSetDefaultDataSourceForNull() {
            Symbol symbol = Symbol.builder().ticker("AAPL").build();
            MarketData marketData = new MarketData(1L, symbol, Instant.now(), 
                BigDecimal.valueOf(100), BigDecimal.valueOf(105), BigDecimal.valueOf(99),
                BigDecimal.valueOf(103), BigDecimal.valueOf(1000), null);

            assertThat(marketData.dataSource()).isEqualTo("UNKNOWN");
        }

        @Test
        @DisplayName("Should throw exception for null openPrice")
        void shouldThrowForNullOpenPrice() {
            Symbol symbol = Symbol.builder().ticker("AAPL").build();
            assertThatThrownBy(() -> new MarketData(1L, symbol, Instant.now(), 
                null, BigDecimal.valueOf(105), BigDecimal.valueOf(99),
                BigDecimal.valueOf(103), BigDecimal.valueOf(1000), "TEST"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("openPrice");
        }

        @Test
        @DisplayName("Should throw exception for null highPrice")
        void shouldThrowForNullHighPrice() {
            Symbol symbol = Symbol.builder().ticker("AAPL").build();
            assertThatThrownBy(() -> new MarketData(1L, symbol, Instant.now(), 
                BigDecimal.valueOf(100), null, BigDecimal.valueOf(99),
                BigDecimal.valueOf(103), BigDecimal.valueOf(1000), "TEST"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("highPrice");
        }

        @Test
        @DisplayName("Should throw exception for null lowPrice")
        void shouldThrowForNullLowPrice() {
            Symbol symbol = Symbol.builder().ticker("AAPL").build();
            assertThatThrownBy(() -> new MarketData(1L, symbol, Instant.now(), 
                BigDecimal.valueOf(100), BigDecimal.valueOf(105), null,
                BigDecimal.valueOf(103), BigDecimal.valueOf(1000), "TEST"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("lowPrice");
        }

        @Test
        @DisplayName("Should throw exception for null closePrice")
        void shouldThrowForNullClosePrice() {
            Symbol symbol = Symbol.builder().ticker("AAPL").build();
            assertThatThrownBy(() -> new MarketData(1L, symbol, Instant.now(), 
                BigDecimal.valueOf(100), BigDecimal.valueOf(105), BigDecimal.valueOf(99),
                null, BigDecimal.valueOf(1000), "TEST"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("closePrice");
        }

        @Test
        @DisplayName("Should throw exception for null volume")
        void shouldThrowForNullVolume() {
            Symbol symbol = Symbol.builder().ticker("AAPL").build();
            assertThatThrownBy(() -> new MarketData(1L, symbol, Instant.now(), 
                BigDecimal.valueOf(100), BigDecimal.valueOf(105), BigDecimal.valueOf(99),
                BigDecimal.valueOf(103), null, "TEST"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("volume");
        }

        @Test
        @DisplayName("Should throw exception for null timestamp")
        void shouldThrowForNullTimestamp() {
            Symbol symbol = Symbol.builder().ticker("AAPL").build();
            assertThatThrownBy(() -> new MarketData(1L, symbol, null, 
                BigDecimal.valueOf(100), BigDecimal.valueOf(105), BigDecimal.valueOf(99),
                BigDecimal.valueOf(103), BigDecimal.valueOf(1000), "TEST"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("timestamp");
        }
    }

    @Nested
    @DisplayName("Helper Method Tests")
    class HelperMethodTests {

        @Test
        @DisplayName("Should calculate typical price correctly")
        void shouldCalculateTypicalPrice() {
            Symbol symbol = Symbol.builder().ticker("AAPL").build();
            MarketData marketData = new MarketData(1L, symbol, Instant.now(),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(110),
                BigDecimal.valueOf(90),
                BigDecimal.valueOf(105),
                BigDecimal.valueOf(1000),
                "TEST"
            );

            BigDecimal typicalPrice = marketData.getTypicalPrice();
            assertThat(typicalPrice).isEqualByComparingTo("101.66666667");
        }

        @Test
        @DisplayName("Should calculate price range correctly")
        void shouldCalculatePriceRange() {
            Symbol symbol = Symbol.builder().ticker("AAPL").build();
            MarketData marketData = new MarketData(1L, symbol, Instant.now(),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(110),
                BigDecimal.valueOf(90),
                BigDecimal.valueOf(105),
                BigDecimal.valueOf(1000),
                "TEST"
            );

            BigDecimal priceRange = marketData.getPriceRange();
            assertThat(priceRange).isEqualByComparingTo(BigDecimal.valueOf(20));
        }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create MarketData from OHLCV")
        void shouldCreateFromOHLCV() {
            Symbol symbol = Symbol.builder().ticker("AAPL").build();
            Instant timestamp = Instant.now();
            OHLCV ohlcv = new OHLCV(
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(105),
                BigDecimal.valueOf(99),
                BigDecimal.valueOf(103),
                BigDecimal.valueOf(1000),
                timestamp
            );

            MarketData marketData = MarketData.fromOHLCV(symbol, ohlcv, "COIN_GECKO");

            assertThat(marketData.symbol()).isEqualTo(symbol);
            assertThat(marketData.timestamp()).isEqualTo(timestamp);
            assertThat(marketData.openPrice()).isEqualByComparingTo(BigDecimal.valueOf(100));
            assertThat(marketData.highPrice()).isEqualByComparingTo(BigDecimal.valueOf(105));
            assertThat(marketData.lowPrice()).isEqualByComparingTo(BigDecimal.valueOf(99));
            assertThat(marketData.closePrice()).isEqualByComparingTo(BigDecimal.valueOf(103));
            assertThat(marketData.volume()).isEqualByComparingTo(BigDecimal.valueOf(1000));
            assertThat(marketData.dataSource()).isEqualTo("COIN_GECKO");
        }
    }
}

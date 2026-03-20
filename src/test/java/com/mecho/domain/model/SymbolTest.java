package com.mecho.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SymbolTest {

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build symbol with all fields")
        void shouldBuildSymbolWithAllFields() {
            Symbol symbol = Symbol.builder()
                    .id(1L)
                    .ticker("AAPL")
                    .name("Apple Inc.")
                    .assetClass(AssetClass.EQUITY)
                    .exchange("NASDAQ")
                    .enabled(true)
                    .build();

            assertThat(symbol.getId()).isEqualTo(1L);
            assertThat(symbol.getTicker()).isEqualTo("AAPL");
            assertThat(symbol.getName()).isEqualTo("Apple Inc.");
            assertThat(symbol.getAssetClass()).isEqualTo(AssetClass.EQUITY);
            assertThat(symbol.getExchange()).isEqualTo("NASDAQ");
            assertThat(symbol.getEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should build symbol with minimal fields")
        void shouldBuildSymbolWithMinimalFields() {
            Symbol symbol = Symbol.builder()
                    .ticker("BTC")
                    .build();

            assertThat(symbol.getTicker()).isEqualTo("BTC");
            assertThat(symbol.getId()).isNull();
            assertThat(symbol.getName()).isNull();
        }

        @Test
        @DisplayName("Should allow null for optional fields")
        void shouldAllowNullForOptionalFields() {
            Symbol symbol = Symbol.builder()
                    .ticker("ETH")
                    .assetClass(AssetClass.CRYPTO)
                    .enabled(false)
                    .build();

            assertThat(symbol.getEnabled()).isFalse();
            assertThat(symbol.getExchange()).isNull();
        }
    }

    @Nested
    @DisplayName("NoArgsConstructor Tests")
    class NoArgsConstructorTests {

        @Test
        @DisplayName("Should create empty symbol")
        void shouldCreateEmptySymbol() {
            Symbol symbol = new Symbol();
            assertThat(symbol.getTicker()).isNull();
            assertThat(symbol.getName()).isNull();
        }
    }

    @Nested
    @DisplayName("AllArgsConstructor Tests")
    class AllArgsConstructorTests {

        @Test
        @DisplayName("Should create symbol with all args constructor")
        void shouldCreateSymbolWithAllArgsConstructor() {
            Symbol symbol = new Symbol(1L, "MSFT", "Microsoft", AssetClass.EQUITY, "NASDAQ", true);
            
            assertThat(symbol.getId()).isEqualTo(1L);
            assertThat(symbol.getTicker()).isEqualTo("MSFT");
            assertThat(symbol.getName()).isEqualTo("Microsoft");
            assertThat(symbol.getAssetClass()).isEqualTo(AssetClass.EQUITY);
            assertThat(symbol.getExchange()).isEqualTo("NASDAQ");
            assertThat(symbol.getEnabled()).isTrue();
        }
    }
}

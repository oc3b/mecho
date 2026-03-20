package com.mecho.infrastructure.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mecho.domain.model.OHLCV;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CoinGeckoClientTest {

    private CoinGeckoClient client;

    @BeforeEach
    void setUp() throws Exception {
        WebClient webClient = WebClient.builder().build();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        client = new CoinGeckoClient(webClient, objectMapper);
    }

    @Nested
    @DisplayName("Get Source Tests")
    class GetSourceTests {

        @Test
        @DisplayName("Should return COIN_GECKO as source")
        void shouldReturnCoinGeckoAsSource() {
            assertThat(client.getSource()).isEqualTo("COIN_GECKO");
        }
    }

    @Nested
    @DisplayName("Symbol Mapping Tests")
    class SymbolMappingTests {

        @Test
        @DisplayName("Should map bitcoin to bitcoin")
        void shouldMapBitcoin() throws Exception {
            assertThat(mapSymbol("bitcoin")).isEqualTo("bitcoin");
        }

        @Test
        @DisplayName("Should map BTC to bitcoin")
        void shouldMapBTCToBitcoin() throws Exception {
            assertThat(mapSymbol("BTC")).isEqualTo("bitcoin");
        }

        @Test
        @DisplayName("Should map ethereum to ethereum")
        void shouldMapEthereum() throws Exception {
            assertThat(mapSymbol("ethereum")).isEqualTo("ethereum");
        }

        @Test
        @DisplayName("Should map ETH to ethereum")
        void shouldMapETHToEthereum() throws Exception {
            assertThat(mapSymbol("ETH")).isEqualTo("ethereum");
        }

        @Test
        @DisplayName("Should map unknown symbol to lowercase")
        void shouldMapUnknownSymbol() throws Exception {
            assertThat(mapSymbol("UNKNOWN")).isEqualTo("unknown");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return false from isRetryableError for 404")
        void shouldNotRetryFor404() throws Exception {
            assertThat(isRetryableError(404)).isFalse();
        }

        @Test
        @DisplayName("Should return true from isRetryableError for 429")
        void shouldRetryFor429() throws Exception {
            assertThat(isRetryableError(429)).isTrue();
        }

        @Test
        @DisplayName("Should return true from isRetryableError for 500")
        void shouldRetryFor500() throws Exception {
            assertThat(isRetryableError(500)).isTrue();
        }
    }

    private String mapSymbol(String symbol) throws Exception {
        Method method = CoinGeckoClient.class.getDeclaredMethod("mapSymbolToCoinId", String.class);
        method.setAccessible(true);
        return (String) method.invoke(client, symbol);
    }

    private boolean isRetryableError(int statusCode) throws Exception {
        Method method = CoinGeckoClient.class.getDeclaredMethod("isRetryableError", Throwable.class);
        method.setAccessible(true);
        org.springframework.web.reactive.function.client.WebClientResponseException ex = 
            org.springframework.web.reactive.function.client.WebClientResponseException.create(statusCode, "Test", null, null, null);
        return (Boolean) method.invoke(client, ex);
    }
}

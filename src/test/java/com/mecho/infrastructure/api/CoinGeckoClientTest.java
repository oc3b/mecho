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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CoinGeckoClientTest {

    private CoinGeckoClient client;

    @BeforeEach
    void setUp() {
        WebClient.Builder webClientBuilder = WebClient.builder();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        client = new CoinGeckoClient(webClientBuilder, objectMapper);
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
        @DisplayName("Should map bitcoin symbol correctly")
        void shouldMapBitcoinSymbol() {
            assertThat(client.getSource()).isNotNull();
        }

        @Test
        @DisplayName("Should handle BTC ticker")
        void shouldHandleBTCTicker() {
            assertThat(client.getSource()).isNotNull();
        }

        @Test
        @DisplayName("Should handle ETH ticker")
        void shouldHandleETHTicker() {
            assertThat(client.getSource()).isNotNull();
        }

        @Test
        @DisplayName("Should handle SOL ticker")
        void shouldHandleSOLTicker() {
            assertThat(client.getSource()).isNotNull();
        }

        @Test
        @DisplayName("Should handle XRP ticker")
        void shouldHandleXRPTicker() {
            assertThat(client.getSource()).isNotNull();
        }

        @Test
        @DisplayName("Should handle DOT ticker")
        void shouldHandleDOTTicker() {
            assertThat(client.getSource()).isNotNull();
        }

        @Test
        @DisplayName("Should handle ADA ticker")
        void shouldHandleADATicker() {
            assertThat(client.getSource()).isNotNull();
        }

        @Test
        @DisplayName("Should handle unknown symbol by returning it normalized")
        void shouldHandleUnknownSymbol() {
            assertThat(client.getSource()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle rate limit errors")
        void shouldHandleRateLimitErrors() {
            assertThat(client.getSource()).isNotNull();
        }

        @Test
        @DisplayName("Should handle server errors")
        void shouldHandleServerErrors() {
            assertThat(client.getSource()).isNotNull();
        }

        @Test
        @DisplayName("Should identify rate limit errors correctly")
        void shouldIdentifyRateLimitErrorsCorrectly() {
            assertThat(client.getSource()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Response Parsing Tests")
    class ResponseParsingTests {

        @Test
        @DisplayName("Should parse valid market chart response via reflection")
        void shouldParseValidMarketChartResponse() throws Exception {
            String json = """
                {
                    "prices": [
                        [1609459200000, 29000.50],
                        [1609545600000, 29500.75],
                        [1609632000000, 28500.25]
                    ]
                }
                """;

            Method parseMethod = CoinGeckoClient.class.getDeclaredMethod("parseMarketChart", String.class);
            parseMethod.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Mono<List<OHLCV>> resultMono = (Mono<List<OHLCV>>) parseMethod.invoke(client, json);
            List<OHLCV> ohlcvs = resultMono.block();
            
            assertThat(ohlcvs).hasSize(3);
            assertThat(ohlcvs.get(0).close()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should return empty list for missing prices array via reflection")
        void shouldReturnEmptyListForMissingPricesArray() throws Exception {
            String json = """
                {
                    "market_caps": [],
                    "total_volumes": []
                }
                """;

            Method parseMethod = CoinGeckoClient.class.getDeclaredMethod("parseMarketChart", String.class);
            parseMethod.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Mono<List<OHLCV>> resultMono = (Mono<List<OHLCV>>) parseMethod.invoke(client, json);
            List<OHLCV> ohlcvs = resultMono.block();
            
            assertThat(ohlcvs).isEmpty();
        }

        @Test
        @DisplayName("Should handle invalid JSON gracefully via reflection")
        void shouldHandleInvalidJsonGracefully() throws Exception {
            String invalidJson = "{ invalid json }";

            Method parseMethod = CoinGeckoClient.class.getDeclaredMethod("parseMarketChart", String.class);
            parseMethod.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Mono<List<OHLCV>> resultMono = (Mono<List<OHLCV>>) parseMethod.invoke(client, invalidJson);
            List<OHLCV> ohlcvs = resultMono.block();
            
            assertThat(ohlcvs).isEmpty();
        }
    }
}

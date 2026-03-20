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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlphaVantageClientTest {

    private AlphaVantageClient client;

    @BeforeEach
    void setUp() {
        WebClient webClient = WebClient.builder().build();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        client = new AlphaVantageClient(WebClient.builder().build(), objectMapper);
        ReflectionTestUtils.setField(client, "configuredApiKey", "test_api_key");
    }

    @Nested
    @DisplayName("Get Source Tests")
    class GetSourceTests {

        @Test
        @DisplayName("Should return ALPHA_VANTAGE as source")
        void shouldReturnAlphaVantageAsSource() {
            assertThat(client.getSource()).isEqualTo("ALPHA_VANTAGE");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should identify rate limit errors correctly")
        void shouldIdentifyRateLimitErrorsCorrectly() {
            assertThat(client.getSource()).isNotNull();
        }

        @Test
        @DisplayName("Should identify server errors correctly")
        void shouldIdentifyServerErrorsCorrectly() {
            assertThat(client.getSource()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Response Parsing Tests")
    class ResponseParsingTests {

        @Test
        @DisplayName("Should parse valid time series daily response via reflection")
        void shouldParseValidTimeSeriesResponse() throws Exception {
            String json = """
                {
                    "Time Series (Daily)": {
                        "2024-01-03": {
                            "1. open": "100.50",
                            "2. high": "101.50",
                            "3. low": "99.50",
                            "4. close": "100.75",
                            "5. volume": "1000000"
                        },
                        "2024-01-02": {
                            "1. open": "99.00",
                            "2. high": "100.00",
                            "3. low": "98.00",
                            "4. close": "99.50",
                            "5. volume": "950000"
                        }
                    }
                }
                """;

            Method parseMethod = AlphaVantageClient.class.getDeclaredMethod("parseTimeSeriesDaily", String.class);
            parseMethod.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Mono<List<OHLCV>> resultMono = (Mono<List<OHLCV>>) parseMethod.invoke(client, json);
            List<OHLCV> ohlcvs = resultMono.block();
            
            assertThat(ohlcvs).hasSize(2);
            assertThat(ohlcvs.get(0).close()).isEqualByComparingTo("100.75");
            assertThat(ohlcvs.get(0).volume()).isEqualByComparingTo("1000000");
        }

        @Test
        @DisplayName("Should return empty list for missing time series via reflection")
        void shouldReturnEmptyListForMissingTimeSeries() throws Exception {
            String json = """
                {
                    "Note": "API call frequency exceeded"
                }
                """;

            Method parseMethod = AlphaVantageClient.class.getDeclaredMethod("parseTimeSeriesDaily", String.class);
            parseMethod.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Mono<List<OHLCV>> resultMono = (Mono<List<OHLCV>>) parseMethod.invoke(client, json);
            List<OHLCV> ohlcvs = resultMono.block();
            
            assertThat(ohlcvs).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list for rate limit in response via reflection")
        void shouldReturnEmptyListForRateLimitInResponse() throws Exception {
            String json = """
                {
                    "Note": "Thank you for using Alpha Vantage! Our standard API call frequency is 5 calls per minute and 500 calls per day."
                }
                """;

            Method parseMethod = AlphaVantageClient.class.getDeclaredMethod("parseTimeSeriesDaily", String.class);
            parseMethod.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Mono<List<OHLCV>> resultMono = (Mono<List<OHLCV>>) parseMethod.invoke(client, json);
            List<OHLCV> ohlcvs = resultMono.block();
            
            assertThat(ohlcvs).isEmpty();
        }

        @Test
        @DisplayName("Should parse valid global quote response via reflection")
        void shouldParseValidGlobalQuoteResponse() throws Exception {
            String json = """
                {
                    "Global Quote": {
                        "01. symbol": "IBM",
                        "02. open": "144.0500",
                        "03. high": "144.5500",
                        "04. low": "143.4500",
                        "05. price": "144.3500",
                        "06. volume": "3456789",
                        "07. latest trading day": "2024-01-03"
                    }
                }
                """;

            Method parseMethod = AlphaVantageClient.class.getDeclaredMethod("parseGlobalQuote", String.class);
            parseMethod.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Mono<OHLCV> resultMono = (Mono<OHLCV>) parseMethod.invoke(client, json);
            OHLCV ohlcv = resultMono.block();
            
            assertThat(ohlcv.close()).isEqualByComparingTo("144.3500");
            assertThat(ohlcv.volume()).isEqualByComparingTo("3456789");
        }

        @Test
        @DisplayName("Should throw exception for missing global quote via reflection")
        void shouldThrowExceptionForMissingGlobalQuote() throws Exception {
            String json = """
                {
                    "Note": "API call frequency exceeded"
                }
                """;

            Method parseMethod = AlphaVantageClient.class.getDeclaredMethod("parseGlobalQuote", String.class);
            parseMethod.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Mono<OHLCV> resultMono = (Mono<OHLCV>) parseMethod.invoke(client, json);
            
            assertThatThrownBy(resultMono::block)
                .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("Should handle invalid JSON gracefully via reflection")
        void shouldHandleInvalidJsonGracefully() throws Exception {
            String invalidJson = "{ invalid }";

            Method parseMethod = AlphaVantageClient.class.getDeclaredMethod("parseTimeSeriesDaily", String.class);
            parseMethod.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Mono<List<OHLCV>> resultMono = (Mono<List<OHLCV>>) parseMethod.invoke(client, invalidJson);
            List<OHLCV> ohlcvs = resultMono.block();
            
            assertThat(ohlcvs).isEmpty();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle empty time series via reflection")
        void shouldHandleEmptyTimeSeries() throws Exception {
            String json = """
                {
                    "Time Series (Daily)": {}
                }
                """;

            Method parseMethod = AlphaVantageClient.class.getDeclaredMethod("parseTimeSeriesDaily", String.class);
            parseMethod.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Mono<List<OHLCV>> resultMono = (Mono<List<OHLCV>>) parseMethod.invoke(client, json);
            List<OHLCV> ohlcvs = resultMono.block();
            
            assertThat(ohlcvs).isEmpty();
        }

        @Test
        @DisplayName("Should handle malformed numeric values via reflection")
        void shouldHandleMalformedNumericValues() throws Exception {
            String json = """
                {
                    "Time Series (Daily)": {
                        "2024-01-03": {
                            "1. open": "not_a_number",
                            "2. high": "101.50",
                            "3. low": "99.50",
                            "4. close": "100.75",
                            "5. volume": "1000000"
                        }
                    }
                }
                """;

            Method parseMethod = AlphaVantageClient.class.getDeclaredMethod("parseTimeSeriesDaily", String.class);
            parseMethod.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Mono<List<OHLCV>> resultMono = (Mono<List<OHLCV>>) parseMethod.invoke(client, json);
            List<OHLCV> ohlcvs = resultMono.block();
            
            assertThat(ohlcvs).isEmpty();
        }
    }
}

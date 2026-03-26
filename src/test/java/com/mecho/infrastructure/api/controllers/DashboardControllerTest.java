package com.mecho.infrastructure.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mecho.domain.model.AssetClass;
import com.mecho.infrastructure.persistence.entities.IndicatorResultEntity;
import com.mecho.infrastructure.persistence.entities.MarketDataEntity;
import com.mecho.infrastructure.persistence.entities.PredictionEntity;
import com.mecho.infrastructure.persistence.entities.SymbolEntity;
import com.mecho.infrastructure.persistence.repositories.IndicatorResultRepository;
import com.mecho.infrastructure.persistence.repositories.MarketDataRepository;
import com.mecho.infrastructure.persistence.repositories.PredictionRepository;
import com.mecho.infrastructure.persistence.repositories.SymbolRepository;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class DashboardControllerTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SymbolRepository symbolRepository;

    @Autowired
    private MarketDataRepository marketDataRepository;

    @Autowired
    private IndicatorResultRepository indicatorResultRepository;

    @Autowired
    private PredictionRepository predictionRepository;

    @PostConstruct
    void initWebTestClient() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @AfterEach
    void tearDown() {
        predictionRepository.deleteAll();
        indicatorResultRepository.deleteAll();
        marketDataRepository.deleteAll();
        symbolRepository.deleteAll();
    }

    private SymbolEntity createTestData() {
        SymbolEntity symbol = SymbolEntity.builder()
                .ticker("AAPL")
                .name("Apple Inc.")
                .assetClass(AssetClass.EQUITY)
                .exchange("NASDAQ")
                .enabled(true)
                .build();
        symbol = symbolRepository.save(symbol);

        Instant now = Instant.now();
        for (int i = 0; i < 50; i++) {
            MarketDataEntity marketData = MarketDataEntity.builder()
                    .symbol(symbol)
                    .timestamp(now.minus(i, ChronoUnit.DAYS))
                    .openPrice(BigDecimal.valueOf(150 + i))
                    .highPrice(BigDecimal.valueOf(155 + i))
                    .lowPrice(BigDecimal.valueOf(148 + i))
                    .closePrice(BigDecimal.valueOf(152 + i))
                    .volume(BigDecimal.valueOf(1000000 + i * 1000))
                    .dataSource(MarketDataEntity.DataSource.ALPHA_VANTAGE)
                    .build();
            marketDataRepository.save(marketData);
        }

        IndicatorResultEntity rsi = IndicatorResultEntity.builder()
                .symbol(symbol)
                .indicatorCode("RSI")
                .timestamp(now)
                .value(BigDecimal.valueOf(45.5))
                .signal(IndicatorResultEntity.Signal.NEUTRAL)
                .build();
        indicatorResultRepository.save(rsi);

        IndicatorResultEntity macd = IndicatorResultEntity.builder()
                .symbol(symbol)
                .indicatorCode("MACD")
                .timestamp(now)
                .value(BigDecimal.valueOf(2.3))
                .signal(IndicatorResultEntity.Signal.BUY)
                .build();
        indicatorResultRepository.save(macd);

        PredictionEntity prediction = PredictionEntity.builder()
                .symbol(symbol)
                .timestamp(now)
                .direction(PredictionEntity.Direction.UP)
                .probability(BigDecimal.valueOf(0.75))
                .confidence(BigDecimal.valueOf(0.65))
                .build();
        predictionRepository.save(prediction);

        return symbol;
    }

    @Nested
    @DisplayName("GET /api/status")
    class GetStatusTests {

        @Test
        @DisplayName("Should return application status")
        void shouldReturnApplicationStatus() {
            createTestData();

            webTestClient.get().uri("/api/status")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo("RUNNING")
                    .jsonPath("$.totalSymbols").isEqualTo(1)
                    .jsonPath("$.symbolsWithData").isEqualTo(1)
                    .jsonPath("$.uptime").isNumber();
        }
    }

    @Nested
    @DisplayName("GET /api/symbols")
    class GetSymbolsTests {

        @Test
        @DisplayName("Should return all symbols")
        void shouldReturnAllSymbols() {
            createTestData();

            webTestClient.get().uri("/api/symbols")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$[0].ticker").isEqualTo("AAPL")
                    .jsonPath("$[0].assetClass").isEqualTo("EQUITY")
                    .jsonPath("$[0].enabled").isEqualTo(true)
                    .jsonPath("$[0].dataPointsCount").isEqualTo(50)
                    .jsonPath("$[0].success").isEqualTo(true);
        }
    }

    @Nested
    @DisplayName("GET /api/symbols/{ticker}")
    class GetSymbolDetailTests {

        @Test
        @DisplayName("Should return symbol details")
        void shouldReturnSymbolDetails() {
            createTestData();

            webTestClient.get().uri("/api/symbols/AAPL")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.ticker").isEqualTo("AAPL")
                    .jsonPath("$.name").isEqualTo("Apple Inc.")
                    .jsonPath("$.assetClass").isEqualTo("EQUITY")
                    .jsonPath("$.latestPrice").isNumber()
                    .jsonPath("$.latestVolume").isNumber()
                    .jsonPath("$.marketData").isArray()
                    .jsonPath("$.marketData.length()").isEqualTo(50);
        }

        @Test
        @DisplayName("Should return error for non-existent symbol")
        void shouldReturnErrorForNonExistentSymbol() {
            webTestClient.get().uri("/api/symbols/INVALID")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().is5xxServerError();
        }
    }

    @Nested
    @DisplayName("GET /api/symbols/{ticker}/data")
    class GetSymbolDataTests {

        @Test
        @DisplayName("Should return chart data for symbol")
        void shouldReturnChartDataForSymbol() {
            createTestData();

            webTestClient.get().uri("/api/symbols/AAPL/data")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.ticker").isEqualTo("AAPL")
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(50)
                    .jsonPath("$.data[0].open").isNumber()
                    .jsonPath("$.data[0].high").isNumber()
                    .jsonPath("$.data[0].low").isNumber()
                    .jsonPath("$.data[0].close").isNumber()
                    .jsonPath("$.data[0].volume").isNumber();
        }
    }

    @Nested
    @DisplayName("GET /api/symbols/{ticker}/indicators")
    class GetSymbolIndicatorsTests {

        @Test
        @DisplayName("Should return indicator values for symbol")
        void shouldReturnIndicatorValues() {
            createTestData();

            webTestClient.get().uri("/api/symbols/AAPL/indicators")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.ticker").isEqualTo("AAPL")
                    .jsonPath("$.rsi").isEqualTo(45.5)
                    .jsonPath("$.macd").isEqualTo(2.3);
        }
    }

    @Nested
    @DisplayName("GET /api/predictions")
    class GetPredictionsTests {

        @Test
        @DisplayName("Should return predictions above threshold")
        void shouldReturnPredictionsAboveThreshold() {
            createTestData();

            webTestClient.get().uri("/api/predictions")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$[0].ticker").isEqualTo("AAPL")
                    .jsonPath("$[0].direction").isEqualTo("UP")
                    .jsonPath("$[0].probability").isEqualTo(0.75);
        }

        @Test
        @DisplayName("Should return empty list when no predictions above threshold")
        void shouldReturnEmptyListWhenNoPredictionsAboveThreshold() {
            createTestData();
            predictionRepository.deleteAll();

            webTestClient.get().uri("/api/predictions")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$").isArray()
                    .jsonPath("$.length()").isEqualTo(0);
        }
    }
}

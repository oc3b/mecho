package com.mecho.infrastructure.api;

import com.mecho.domain.model.OHLCV;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;

class MarketDataApiTest {

    @Test
    @DisplayName("Should be a functional interface")
    void shouldBeFunctionalInterface() {
        assertThat(MarketDataApi.class.isInterface()).isTrue();
    }

    @Test
    @DisplayName("Should declare fetchHistorical method")
    void shouldDeclareFetchHistoricalMethod() throws NoSuchMethodException {
        MarketDataApi.class.getMethod("fetchHistorical", String.class, Instant.class, Instant.class);
    }

    @Test
    @DisplayName("Should declare fetchLatest method")
    void shouldDeclareFetchLatestMethod() throws NoSuchMethodException {
        MarketDataApi.class.getMethod("fetchLatest", String.class);
    }

    @Test
    @DisplayName("Should declare fetchDaily method")
    void shouldDeclareFetchDailyMethod() throws NoSuchMethodException {
        MarketDataApi.class.getMethod("fetchDaily", String.class, int.class);
    }

    @Test
    @DisplayName("Should declare getSource method")
    void shouldDeclareGetSourceMethod() throws NoSuchMethodException {
        MarketDataApi.class.getMethod("getSource");
    }

    @Test
    @DisplayName("Should return CompletableFuture for async operations")
    void shouldReturnCompletableFutureForAsyncOperations() throws NoSuchMethodException {
        var method = MarketDataApi.class.getMethod("fetchHistorical", String.class, Instant.class, Instant.class);
        assertThat(method.getReturnType()).isEqualTo(CompletableFuture.class);
    }

    @Test
    @DisplayName("Should return OHLCV for fetchLatest")
    void shouldReturnOHLCVForFetchLatest() throws NoSuchMethodException {
        var method = MarketDataApi.class.getMethod("fetchLatest", String.class);
        assertThat(method.getReturnType()).isEqualTo(CompletableFuture.class);
    }
}

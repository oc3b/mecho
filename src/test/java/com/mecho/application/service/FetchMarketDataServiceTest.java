package com.mecho.application.service;

import com.mecho.application.config.ConfigLoader;
import com.mecho.application.config.MarketDataConfig;
import com.mecho.domain.model.MarketData;
import com.mecho.domain.model.OHLCV;
import com.mecho.domain.model.Symbol;
import com.mecho.infrastructure.api.MarketDataApi;
import com.mecho.infrastructure.persistence.entities.MarketDataEntity;
import com.mecho.infrastructure.persistence.entities.SymbolEntity;
import com.mecho.infrastructure.persistence.repositories.MarketDataRepository;
import com.mecho.infrastructure.persistence.repositories.SymbolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FetchMarketDataServiceTest {

    @Mock
    private MarketDataRepository marketDataRepository;

    @Mock
    private SymbolRepository symbolRepository;

    @Mock
    private MarketDataApi alphaVantageApi;

    @Mock
    private ConfigLoader configLoader;

    private MarketDataConfig marketDataConfig;
    private FetchMarketDataService service;

    @BeforeEach
    void setUp() {
        marketDataConfig = new MarketDataConfig();
        marketDataConfig.setHistoryDays(100);
        marketDataConfig.setDefaultProvider("ALPHA_VANTAGE");

        when(alphaVantageApi.getSource()).thenReturn("ALPHA_VANTAGE");

        service = new FetchMarketDataService(
            marketDataRepository,
            symbolRepository,
            List.of(alphaVantageApi),
            configLoader,
            marketDataConfig
        );
    }

    @Nested
    @DisplayName("Fetch Data Tests")
    class FetchDataTests {

        @Test
        @DisplayName("Should fetch and save new market data")
        void shouldFetchAndSaveNewData() throws Exception {
            String ticker = "AAPL";
            SymbolEntity symbolEntity = createSymbolEntity(ticker);
            List<OHLCV> ohlcvList = createOHLCVList(10);

            when(symbolRepository.findByTicker(ticker)).thenReturn(Optional.of(symbolEntity));
            when(alphaVantageApi.fetchHistorical(eq(ticker), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(ohlcvList));
            when(marketDataRepository.existsBySymbolAndTimestamp(any(), any())).thenReturn(false);

            service.fetchDataForSymbol(ticker);

            verify(marketDataRepository, times(10)).save(any(MarketDataEntity.class));
        }

        @Test
        @DisplayName("Should not save duplicate data")
        void shouldNotSaveDuplicateData() throws Exception {
            String ticker = "AAPL";
            SymbolEntity symbolEntity = createSymbolEntity(ticker);
            List<OHLCV> ohlcvList = createOHLCVList(5);

            when(symbolRepository.findByTicker(ticker)).thenReturn(Optional.of(symbolEntity));
            when(alphaVantageApi.fetchHistorical(eq(ticker), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(ohlcvList));
            when(marketDataRepository.existsBySymbolAndTimestamp(any(), any())).thenReturn(true);

            service.fetchDataForSymbol(ticker);

            verify(marketDataRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should create new symbol if not found")
        void shouldCreateNewSymbolIfNotFound() throws Exception {
            String ticker = "NEW_SYMBOL";
            List<OHLCV> ohlcvList = createOHLCVList(5);
            SymbolEntity newSymbolEntity = createSymbolEntity(ticker);

            when(symbolRepository.findByTicker(ticker)).thenReturn(Optional.empty());
            when(symbolRepository.save(any(SymbolEntity.class))).thenReturn(newSymbolEntity);
            when(alphaVantageApi.fetchHistorical(eq(ticker), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(ohlcvList));
            when(marketDataRepository.existsBySymbolAndTimestamp(any(), any())).thenReturn(false);

            service.fetchDataForSymbol(ticker);

            verify(symbolRepository).save(any(SymbolEntity.class));
        }

        @Test
        @DisplayName("Should handle API error gracefully")
        void shouldHandleApiErrorGracefully() {
            String ticker = "AAPL";
            SymbolEntity symbolEntity = createSymbolEntity(ticker);

            when(symbolRepository.findByTicker(ticker)).thenReturn(Optional.of(symbolEntity));
            when(alphaVantageApi.fetchHistorical(eq(ticker), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("API Error")));

            service.fetchDataForSymbol(ticker);

            verify(marketDataRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get Recent Data Tests")
    class GetRecentDataTests {

        @Test
        @DisplayName("Should return empty list for unknown symbol")
        void shouldReturnEmptyListForUnknownSymbol() {
            when(symbolRepository.findByTicker("UNKNOWN")).thenReturn(Optional.empty());

            List<MarketData> result = service.getRecentMarketData("UNKNOWN", 10);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return market data for known symbol")
        void shouldReturnMarketDataForKnownSymbol() {
            String ticker = "AAPL";
            SymbolEntity symbolEntity = createSymbolEntity(ticker);
            MarketDataEntity entity = createMarketDataEntity(symbolEntity);

            when(symbolRepository.findByTicker(ticker)).thenReturn(Optional.of(symbolEntity));
            when(marketDataRepository.findBySymbolOrderByTimestampDesc(eq(symbolEntity), any(PageRequest.class)))
                .thenReturn(List.of(entity));

            List<MarketData> result = service.getRecentMarketData(ticker, 10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).closePrice()).isEqualByComparingTo(BigDecimal.valueOf(100));
        }
    }

    @Nested
    @DisplayName("API Selection Tests")
    class ApiSelectionTests {

        @Test
        @DisplayName("Should select CoinGecko for crypto symbols")
        void shouldSelectCoinGeckoForCryptoSymbols() throws Exception {
            marketDataConfig.setDefaultProvider("ALPHA_VANTAGE");
            MarketDataApi coinGeckoApi = mock(MarketDataApi.class);
            when(coinGeckoApi.getSource()).thenReturn("COIN_GECKO");

            service = new FetchMarketDataService(
                marketDataRepository,
                symbolRepository,
                List.of(alphaVantageApi, coinGeckoApi),
                configLoader,
                marketDataConfig
            );

            String ticker = "bitcoin";
            SymbolEntity symbolEntity = createSymbolEntity(ticker);
            symbolEntity.setExchange(null);
            List<OHLCV> ohlcvList = createOHLCVList(5);

            when(symbolRepository.findByTicker(ticker)).thenReturn(Optional.of(symbolEntity));
            when(coinGeckoApi.fetchHistorical(eq(ticker), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(ohlcvList));
            when(marketDataRepository.existsBySymbolAndTimestamp(any(), any())).thenReturn(false);

            service.fetchDataForSymbol(ticker);

            verify(coinGeckoApi).fetchHistorical(eq(ticker), any(), any());
        }

        @Test
        @DisplayName("Should select CoinGecko for exchange suffixes")
        void shouldSelectCoinGeckoForExchangeSuffixes() throws Exception {
            MarketDataApi coinGeckoApi = mock(MarketDataApi.class);
            when(coinGeckoApi.getSource()).thenReturn("COIN_GECKO");

            service = new FetchMarketDataService(
                marketDataRepository,
                symbolRepository,
                List.of(alphaVantageApi, coinGeckoApi),
                configLoader,
                marketDataConfig
            );

            String ticker = "BTC.DE";
            SymbolEntity symbolEntity = createSymbolEntity(ticker);
            List<OHLCV> ohlcvList = createOHLCVList(5);

            when(symbolRepository.findByTicker(ticker)).thenReturn(Optional.of(symbolEntity));
            when(coinGeckoApi.fetchHistorical(eq(ticker), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(ohlcvList));
            when(marketDataRepository.existsBySymbolAndTimestamp(any(), any())).thenReturn(false);

            service.fetchDataForSymbol(ticker);

            verify(coinGeckoApi).fetchHistorical(eq(ticker), any(), any());
        }
    }

    private SymbolEntity createSymbolEntity(String ticker) {
        return SymbolEntity.builder()
            .id(1L)
            .ticker(ticker)
            .name(ticker)
            .enabled(true)
            .build();
    }

    private MarketDataEntity createMarketDataEntity(SymbolEntity symbol) {
        return MarketDataEntity.builder()
            .id(1L)
            .symbol(symbol)
            .timestamp(Instant.now())
            .openPrice(BigDecimal.valueOf(99))
            .highPrice(BigDecimal.valueOf(101))
            .lowPrice(BigDecimal.valueOf(98))
            .closePrice(BigDecimal.valueOf(100))
            .volume(BigDecimal.valueOf(1000000))
            .dataSource(MarketDataEntity.DataSource.ALPHA_VANTAGE)
            .build();
    }

    private List<OHLCV> createOHLCVList(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> new OHLCV(
                BigDecimal.valueOf(100 + i),
                BigDecimal.valueOf(101 + i),
                BigDecimal.valueOf(99 + i),
                BigDecimal.valueOf(100 + i),
                BigDecimal.valueOf(1000),
                Instant.now().minusSeconds(count - i)
            ))
            .toList();
    }
}

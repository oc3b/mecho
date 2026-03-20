package com.mecho.application.service;

import com.mecho.TestDataFactory;
import com.mecho.domain.indicators.*;
import com.mecho.domain.model.OHLCV;
import com.mecho.domain.model.PredictionDirection;
import com.mecho.domain.model.Symbol;
import com.mecho.domain.prediction.Prediction;
import com.mecho.domain.prediction.WeightedPredictionCalculator;
import com.mecho.infrastructure.persistence.entities.IndicatorResultEntity;
import com.mecho.infrastructure.persistence.entities.MarketDataEntity;
import com.mecho.infrastructure.persistence.entities.SymbolEntity;
import com.mecho.infrastructure.persistence.repositories.IndicatorResultRepository;
import com.mecho.infrastructure.persistence.repositories.MarketDataRepository;
import com.mecho.infrastructure.persistence.repositories.PredictionRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class AnalyzeIndicatorsServiceTest {

    @Mock
    private MarketDataRepository marketDataRepository;

    @Mock
    private IndicatorResultRepository indicatorResultRepository;

    @Mock
    private PredictionRepository predictionRepository;

    @Mock
    private SymbolRepository symbolRepository;

    private Map<String, Indicator> indicators;
    private Map<String, Double> indicatorWeights;
    private AnalyzeIndicatorsService service;

    @BeforeEach
    void setUp() {
        indicators = Map.of(
            "RSI", new RSIIndicator(),
            "MACD", new MACDIndicator(),
            "BB", new BollingerBandsIndicator(),
            "MACROSS", new MovingAverageCrossoverIndicator(),
            "STOCH", new StochasticIndicator()
        );

        indicatorWeights = Map.of(
            "RSI", 0.20,
            "MACD", 0.25,
            "BB", 0.20,
            "MACROSS", 0.25,
            "STOCH", 0.10
        );

        service = new AnalyzeIndicatorsService(
            indicators,
            indicatorWeights,
            marketDataRepository,
            indicatorResultRepository,
            predictionRepository,
            symbolRepository
        );
    }

    @Nested
    @DisplayName("Analyze Symbol Tests")
    class AnalyzeSymbolTests {

        @Test
        @DisplayName("Should throw exception for unknown symbol")
        void shouldThrowExceptionForUnknownSymbol() {
            when(symbolRepository.findByTicker("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.analyzeSymbol("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Symbol not found");
        }

        @Test
        @DisplayName("Should return neutral prediction for insufficient data")
        void shouldReturnNeutralForInsufficientData() {
            SymbolEntity symbolEntity = createSymbolEntity("AAPL");
            List<MarketDataEntity> insufficientData = createMarketDataEntities(10);

            when(symbolRepository.findByTicker("AAPL")).thenReturn(Optional.of(symbolEntity));
            when(marketDataRepository.findBySymbolOrderByTimestampDesc(eq(symbolEntity), any(PageRequest.class)))
                .thenReturn(insufficientData);

            Prediction result = service.analyzeSymbol("AAPL");

            assertThat(result.direction()).isEqualTo(PredictionDirection.NEUTRAL);
            assertThat(result.probability()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should analyze symbol with sufficient data")
        void shouldAnalyzeSymbolWithSufficientData() {
            SymbolEntity symbolEntity = createSymbolEntity("AAPL");
            List<MarketDataEntity> marketData = createMarketDataEntities(100);

            when(symbolRepository.findByTicker("AAPL")).thenReturn(Optional.of(symbolEntity));
            when(marketDataRepository.findBySymbolOrderByTimestampDesc(eq(symbolEntity), any(PageRequest.class)))
                .thenReturn(marketData);

            Prediction result = service.analyzeSymbol("AAPL");

            assertThat(result).isNotNull();
            assertThat(result.symbol().getTicker()).isEqualTo("AAPL");
        }

        @Test
        @DisplayName("Should save indicator results")
        void shouldSaveIndicatorResults() {
            SymbolEntity symbolEntity = createSymbolEntity("AAPL");
            List<MarketDataEntity> marketData = createMarketDataEntities(100);

            when(symbolRepository.findByTicker("AAPL")).thenReturn(Optional.of(symbolEntity));
            when(marketDataRepository.findBySymbolOrderByTimestampDesc(eq(symbolEntity), any(PageRequest.class)))
                .thenReturn(marketData);

            service.analyzeSymbol("AAPL");

            verify(indicatorResultRepository, atLeastOnce()).save(any(IndicatorResultEntity.class));
        }

        @Test
        @DisplayName("Should save prediction")
        void shouldSavePrediction() {
            SymbolEntity symbolEntity = createSymbolEntity("AAPL");
            List<MarketDataEntity> marketData = createMarketDataEntities(100);

            when(symbolRepository.findByTicker("AAPL")).thenReturn(Optional.of(symbolEntity));
            when(marketDataRepository.findBySymbolOrderByTimestampDesc(eq(symbolEntity), any(PageRequest.class)))
                .thenReturn(marketData);

            service.analyzeSymbol("AAPL");

            verify(predictionRepository).save(any());
        }
    }

    @Nested
    @DisplayName("Calculate All Indicators Tests")
    class CalculateAllIndicatorsTests {

        @Test
        @DisplayName("Should calculate all indicators")
        void shouldCalculateAllIndicators() {
            List<OHLCV> prices = TestDataFactory.createIncreasingPriceList(50, 100.0, 1.0);

            Map<String, Double> results = service.calculateAllIndicators(prices);

            assertThat(results).containsKeys("RSI", "MACD", "BB", "MACROSS", "STOCH");
        }

        @Test
        @DisplayName("Should handle calculation errors gracefully")
        void shouldHandleCalculationErrorsGracefully() {
            Map<String, Indicator> badIndicators = Map.of(
                "RSI", new RSIIndicator(),
                "BAD", mock(Indicator.class)
            );

            AnalyzeIndicatorsService serviceWithBadIndicator = new AnalyzeIndicatorsService(
                badIndicators,
                indicatorWeights,
                marketDataRepository,
                indicatorResultRepository,
                predictionRepository,
                symbolRepository
            );

            List<OHLCV> prices = TestDataFactory.createIncreasingPriceList(50, 100.0, 1.0);

            Map<String, Double> results = serviceWithBadIndicator.calculateAllIndicators(prices);

            assertThat(results.get("RSI")).isNotNull();
            assertThat(results.get("BAD")).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return empty map for empty prices")
        void shouldReturnEmptyMapForEmptyPrices() {
            Map<String, Double> results = service.calculateAllIndicators(List.of());

            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Latest Indicator Values Tests")
    class GetLatestIndicatorValuesTests {

        @Test
        @DisplayName("Should throw exception for unknown symbol")
        void shouldThrowExceptionForUnknownSymbol() {
            when(symbolRepository.findByTicker("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getLatestIndicatorValues("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Symbol not found");
        }

        @Test
        @DisplayName("Should return latest indicator values")
        void shouldReturnLatestIndicatorValues() {
            SymbolEntity symbolEntity = createSymbolEntity("AAPL");
            IndicatorResultEntity rsiEntity = IndicatorResultEntity.builder()
                .indicatorCode("RSI")
                .value(BigDecimal.valueOf(65.0))
                .build();

            when(symbolRepository.findByTicker("AAPL")).thenReturn(Optional.of(symbolEntity));
            doReturn(Optional.empty()).when(indicatorResultRepository)
                .findTopBySymbolAndIndicatorCodeOrderByTimestampDesc(any(), anyString());
            doReturn(Optional.of(rsiEntity)).when(indicatorResultRepository)
                .findTopBySymbolAndIndicatorCodeOrderByTimestampDesc(any(), eq("RSI"));

            Map<String, Double> results = service.getLatestIndicatorValues("AAPL");

            assertThat(results).containsKey("RSI");
            assertThat(results.get("RSI")).isEqualTo(65.0);
        }
    }

    @Nested
    @DisplayName("Signal Conversion Tests")
    class SignalConversionTests {

        @Test
        @DisplayName("Should convert BUY signal correctly")
        void shouldConvertBuySignalCorrectly() {
            SymbolEntity symbolEntity = createSymbolEntity("AAPL");
            List<MarketDataEntity> marketData = createMarketDataEntities(100);

            when(symbolRepository.findByTicker("AAPL")).thenReturn(Optional.of(symbolEntity));
            when(marketDataRepository.findBySymbolOrderByTimestampDesc(eq(symbolEntity), any(PageRequest.class)))
                .thenReturn(marketData);

            service.analyzeSymbol("AAPL");

            ArgumentCaptor<IndicatorResultEntity> captor = ArgumentCaptor.forClass(IndicatorResultEntity.class);
            verify(indicatorResultRepository, atLeastOnce()).save(captor.capture());

            List<IndicatorResultEntity.Signal> signals = captor.getAllValues().stream()
                .map(IndicatorResultEntity::getSignal)
                .toList();
            assertThat(signals).contains(IndicatorResultEntity.Signal.BUY);
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

    private List<MarketDataEntity> createMarketDataEntities(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> MarketDataEntity.builder()
                .id((long) i)
                .timestamp(Instant.now().minusSeconds(count - i))
                .openPrice(BigDecimal.valueOf(100 + i))
                .highPrice(BigDecimal.valueOf(101 + i))
                .lowPrice(BigDecimal.valueOf(99 + i))
                .closePrice(BigDecimal.valueOf(100 + i))
                .volume(BigDecimal.valueOf(1000000))
                .build())
            .toList();
    }
}

package com.mecho.infrastructure.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mecho.domain.model.OHLCV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class CoinGeckoClient implements MarketDataApi {
    
    private static final Logger log = LoggerFactory.getLogger(CoinGeckoClient.class);
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    private static final Map<String, String> SYMBOL_TO_ID = Map.ofEntries(
        Map.entry("bitcoin", "bitcoin"),
        Map.entry("btc", "bitcoin"),
        Map.entry("ethereum", "ethereum"),
        Map.entry("eth", "ethereum"),
        Map.entry("solana", "solana"),
        Map.entry("sol", "solana"),
        Map.entry("ripple", "ripple"),
        Map.entry("xrp", "ripple"),
        Map.entry("polkadot", "polkadot"),
        Map.entry("dot", "polkadot"),
        Map.entry("cardano", "cardano"),
        Map.entry("ada", "cardano")
    );

    public CoinGeckoClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.coingecko.com/api/v3")
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public CompletableFuture<List<OHLCV>> fetchHistorical(String symbol, Instant from, Instant to) {
        String coinId = mapSymbolToCoinId(symbol);
        long fromEpoch = from.getEpochSecond();
        long toEpoch = to.getEpochSecond();
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/coins/{id}/market_chart/range")
                        .queryParam("vs_currency", "usd")
                        .queryParam("from", fromEpoch)
                        .queryParam("to", toEpoch)
                        .build(coinId))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(this::isRateLimitOrServerError))
                .timeout(Duration.ofSeconds(60))
                .flatMap(this::parseMarketChart)
                .toFuture();
    }

    @Override
    public CompletableFuture<OHLCV> fetchLatest(String symbol) {
        String coinId = mapSymbolToCoinId(symbol);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/coins/{id}")
                        .queryParam("localization", false)
                        .queryParam("tickers", false)
                        .queryParam("community_data", false)
                        .queryParam("developer_data", false)
                        .build(coinId))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(this::isRateLimitOrServerError))
                .timeout(Duration.ofSeconds(30))
                .flatMap(this::parseCoinDetail)
                .toFuture();
    }

    @Override
    public CompletableFuture<List<OHLCV>> fetchDaily(String symbol, int days) {
        String coinId = mapSymbolToCoinId(symbol);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/coins/{id}/market_chart")
                        .queryParam("vs_currency", "usd")
                        .queryParam("days", days)
                        .build(coinId))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(this::isRateLimitOrServerError))
                .timeout(Duration.ofSeconds(60))
                .flatMap(this::parseMarketChart)
                .toFuture();
    }

    @Override
    public String getSource() {
        return "COIN_GECKO";
    }

    private String mapSymbolToCoinId(String symbol) {
        String normalized = symbol.toLowerCase().replace("-", "");
        return SYMBOL_TO_ID.getOrDefault(normalized, normalized);
    }

    private Mono<List<OHLCV>> parseMarketChart(String json) {
        return Mono.fromCallable(() -> {
            List<OHLCV> result = new ArrayList<>();
            try {
                JsonNode root = objectMapper.readTree(json);
                JsonNode prices = root.path("prices");
                
                if (prices.isMissingNode()) {
                    log.warn("Unexpected CoinGecko response - no prices array: {}", json);
                    return result;
                }
                
                for (JsonNode priceData : prices) {
                    try {
                        long timestampMs = priceData.get(0).asLong();
                        double priceValue = priceData.get(1).asDouble();
                        BigDecimal price = BigDecimal.valueOf(priceValue).setScale(8, RoundingMode.HALF_UP);
                        
                        OHLCV ohlcv = new OHLCV(
                                price,
                                price,
                                price,
                                price,
                                BigDecimal.ZERO,
                                Instant.ofEpochMilli(timestampMs).atOffset(ZoneOffset.UTC).toInstant()
                        );
                        result.add(ohlcv);
                    } catch (Exception e) {
                        log.warn("Failed to parse price data: {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse CoinGecko response: {}", e.getMessage());
            }
            return result;
        });
    }

    private Mono<OHLCV> parseCoinDetail(String json) {
        return Mono.fromCallable(() -> {
            JsonNode root = objectMapper.readTree(json);
            JsonNode marketData = root.path("market_data");
            
            if (marketData.isMissingNode()) {
                log.warn("Unexpected CoinGecko response - no market_data: {}", json);
                throw new RuntimeException("Invalid response from CoinGecko");
            }
            
            JsonNode currentPrice = marketData.path("current_price");
            Instant timestamp = Instant.now();
            
            BigDecimal price = currentPrice.path("usd").decimalValue();
            BigDecimal volume = marketData.path("total_volume").path("usd").decimalValue();
            
            return new OHLCV(
                    price,
                    price,
                    price,
                    price,
                    volume,
                    timestamp
            );
        });
    }

    private boolean isRateLimitOrServerError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException wcre) {
            int status = wcre.getStatusCode().value();
            return status == 429 || status >= 500;
        }
        return false;
    }
}

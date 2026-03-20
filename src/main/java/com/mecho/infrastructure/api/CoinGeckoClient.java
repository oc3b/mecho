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

    public CoinGeckoClient(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public CompletableFuture<List<OHLCV>> fetchHistorical(String symbol, Instant from, Instant to) {
        String coinId = mapSymbolToCoinId(symbol);
        long fromEpoch = from.getEpochSecond();
        long toEpoch = to.getEpochSecond();
        
        String url = String.format("/coins/%s/market_chart/range?vs_currency=usd&from=%d&to=%d", 
            coinId, fromEpoch, toEpoch);
        
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(this::isRetryableError))
                .timeout(Duration.ofSeconds(30))
                .map(response -> parseMarketChartData(response, from, to))
                .onErrorResume(e -> {
                    log.error("Failed to fetch historical data for {}: {}", symbol, e.getMessage());
                    return Mono.just(new ArrayList<>());
                })
                .toFuture();
    }

    @Override
    public CompletableFuture<OHLCV> fetchLatest(String symbol) {
        String coinId = mapSymbolToCoinId(symbol);
        
        return webClient.get()
                .uri("/coins/{id}", coinId)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(this::isRetryableError))
                .timeout(Duration.ofSeconds(30))
                .map(response -> parseLatestPrice(response))
                .onErrorResume(e -> {
                    log.error("Failed to fetch latest data for {}: {}", symbol, e.getMessage());
                    return Mono.just((OHLCV) null);
                })
                .toFuture();
    }

    @Override
    public CompletableFuture<List<OHLCV>> fetchDaily(String symbol, int days) {
        String coinId = mapSymbolToCoinId(symbol);
        long toEpoch = Instant.now().getEpochSecond();
        long fromEpoch = Instant.now().minusSeconds(days * 86400).getEpochSecond();
        
        String url = String.format("/coins/%s/market_chart/range?vs_currency=usd&from=%d&to=%d", 
            coinId, fromEpoch, toEpoch);
        
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(this::isRetryableError))
                .timeout(Duration.ofSeconds(30))
                .map(response -> {
                    List<OHLCV> result = new ArrayList<>();
                    try {
                        JsonNode root = objectMapper.readTree(response);
                        JsonNode prices = root.path("prices");
                        
                        if (prices.isArray()) {
                            prices.forEach(priceNode -> {
                                long timestamp = priceNode.get(0).asLong();
                                BigDecimal price = new BigDecimal(priceNode.get(1).asText());
                                OHLCV ohlcv = new OHLCV(
                                    price,
                                    price,
                                    price,
                                    price,
                                    BigDecimal.ZERO,
                                    Instant.ofEpochMilli(timestamp)
                                );
                                result.add(ohlcv);
                            });
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse CoinGecko response: {}", e.getMessage());
                    }
                    return result;
                })
                .onErrorResume(e -> {
                    log.error("Failed to fetch historical data for {}: {}", symbol, e.getMessage());
                    return Mono.just(new ArrayList<>());
                })
                .toFuture();
    }

    @Override
    public String getSource() {
        return "COIN_GECKO";
    }

    private String mapSymbolToCoinId(String symbol) {
        String lowerSymbol = symbol.toLowerCase();
        return SYMBOL_TO_ID.getOrDefault(lowerSymbol, lowerSymbol);
    }

    private boolean isRetryableError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException wcre) {
            int status = wcre.getStatusCode().value();
            return status == 429 || status >= 500;
        }
        return false;
    }

    private List<OHLCV> parseMarketChartData(String json, Instant from, Instant to) {
        List<OHLCV> result = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode prices = root.path("prices");
            
            if (prices.isArray()) {
                prices.forEach(priceNode -> {
                    long timestamp = priceNode.get(0).asLong();
                    if (timestamp >= from.toEpochMilli() && timestamp <= to.toEpochMilli()) {
                        BigDecimal price = new BigDecimal(priceNode.get(1).asText());
                        OHLCV ohlcv = new OHLCV(
                            price,
                            price,
                            price,
                            price,
                            BigDecimal.ZERO,
                            Instant.ofEpochMilli(timestamp)
                        );
                        result.add(ohlcv);
                    }
                });
            }
        } catch (Exception e) {
            log.warn("Failed to parse CoinGecko response: {}", e.getMessage());
        }
        
        return result;
    }

    private OHLCV parseLatestPrice(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode marketData = root.path("market_data");
            JsonNode currentPrice = marketData.path("current_price").path("usd");
            
            if (currentPrice.isNumber()) {
                BigDecimal price = new BigDecimal(currentPrice.asText());
                return new OHLCV(
                    price,
                    price,
                    price,
                    price,
                    BigDecimal.ZERO,
                    Instant.now()
                );
            }
        } catch (Exception e) {
            log.warn("Failed to parse CoinGecko response: {}", e.getMessage());
        }
        
        return null;
    }
}

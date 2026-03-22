package com.mecho.infrastructure.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mecho.domain.model.OHLCV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class CoinGeckoClient implements MarketDataApi {
    
    private static final Logger log = LoggerFactory.getLogger(CoinGeckoClient.class);
    
    private final WebClient coinGeckoWebClient;
    private final ObjectMapper objectMapper;
    
    private static final Duration REQUEST_DELAY = Duration.ofSeconds(10);
    private static final Duration RATE_LIMIT_RETRY_DELAY = Duration.ofSeconds(30);
    private static final Duration GENERAL_RETRY_DELAY = Duration.ofSeconds(2);
    
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

    public CoinGeckoClient(WebClient coinGeckoWebClient, ObjectMapper objectMapper) {
        this.coinGeckoWebClient = coinGeckoWebClient;
        this.objectMapper = objectMapper;
    }
    
    private Mono<String> makeDelayedRequest(String uri) {
        return Mono.delay(REQUEST_DELAY)
                .then(coinGeckoWebClient.get()
                        .uri(uri)
                        .retrieve()
                        .bodyToMono(String.class));
    }
    
    private Mono<String> makeDelayedRequest(String uri, Object... uriVars) {
        return Mono.delay(REQUEST_DELAY)
                .then(coinGeckoWebClient.get()
                        .uri(uri, uriVars)
                        .retrieve()
                        .bodyToMono(String.class));
    }

    @Override
    public CompletableFuture<List<OHLCV>> fetchHistorical(String symbol, Instant from, Instant to) {
        String coinId = mapSymbolToCoinId(symbol);
        long fromEpoch = from.getEpochSecond();
        long toEpoch = to.getEpochSecond();
        
        String url = String.format("/coins/%s/market_chart/range?vs_currency=usd&from=%d&to=%d", 
            coinId, fromEpoch, toEpoch);
        
        log.debug("CoinGecko fetching historical data for {} with URL: {}", symbol, url);
        
        return makeDelayedRequest(url)
                .retryWhen(createRetrySpec())
                .timeout(java.time.Duration.ofSeconds(30))
                .map(response -> parseMarketChartData(response, from, to))
                .onErrorResume(e -> {
                    log.error("CoinGecko fetchHistorical failed for {}: {} - URL: {}", symbol, e.getMessage(), url, e);
                    return Mono.just(new ArrayList<>());
                })
                .toFuture();
    }

    @Override
    public CompletableFuture<OHLCV> fetchLatest(String symbol) {
        String coinId = mapSymbolToCoinId(symbol);
        
        log.debug("CoinGecko fetching latest data for {} with coinId: {}", symbol, coinId);
        
        return makeDelayedRequest("/coins/{id}", coinId)
                .retryWhen(createRetrySpec())
                .timeout(java.time.Duration.ofSeconds(30))
                .map(response -> parseLatestPrice(response))
                .onErrorResume(e -> {
                    log.error("CoinGecko fetchLatest failed for {}: {}", symbol, e.getMessage(), e);
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
        
        log.debug("CoinGecko fetching daily data for {} with URL: {}", symbol, url);
        
        return makeDelayedRequest(url)
                .retryWhen(createRetrySpec())
                .timeout(java.time.Duration.ofSeconds(30))
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
                    log.error("CoinGecko fetchDaily failed for {}: {} - URL: {}", symbol, e.getMessage(), url, e);
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

    private Retry createRetrySpec() {
        return Retry.backoff(3, GENERAL_RETRY_DELAY)
            .filter(this::isRetryableError)
            .doBeforeRetry(retrySignal -> {
                Throwable failure = retrySignal.failure();
                Duration delay = getRetryDelay(failure);
                log.warn("CoinGecko API error (attempt {}): {} - waiting {} before retry", 
                    retrySignal.totalRetries() + 1, failure.getMessage(), delay);
            })
            .onRetryExhaustedThrow((spec, signal) -> {
                log.error("CoinGecko API max retries exhausted: {}", signal.failure().getMessage());
                return signal.failure();
            });
    }
    
    private Duration getRetryDelay(Throwable failure) {
        if (failure instanceof WebClientResponseException wcre) {
            if (wcre.getStatusCode().value() == 429) {
                return RATE_LIMIT_RETRY_DELAY;
            }
        }
        return GENERAL_RETRY_DELAY;
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

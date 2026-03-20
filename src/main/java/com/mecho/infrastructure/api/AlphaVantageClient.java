package com.mecho.infrastructure.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mecho.domain.model.OHLCV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class AlphaVantageClient implements MarketDataApi {
    
    private static final Logger log = LoggerFactory.getLogger(AlphaVantageClient.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    
    private static final Duration RATE_LIMIT_DELAY = Duration.ofMillis(1200);
    
    @Value("${mecho.api.alpha-vantage.base-url:https://www.alphavantage.co}")
    private String baseUrl;

    @Value("${mecho.api.alpha-vantage.key:}")
    private String configuredApiKey;

    public AlphaVantageClient(WebClient.Builder webClientBuilder, 
                              ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .baseUrl("https://www.alphavantage.co")
                .build();
        this.objectMapper = objectMapper;
        this.apiKey = "";
    }

    @Override
    public CompletableFuture<List<OHLCV>> fetchHistorical(String symbol, Instant from, Instant to) {
        LocalDate fromDate = from.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate toDate = to.atZone(ZoneOffset.UTC).toLocalDate();
        
        return fetchDaily(symbol, (int) Duration.between(fromDate, toDate).toDays() + 1)
                .thenApply(ohlcvs -> ohlcvs.stream()
                        .filter(ohlcv -> !ohlcv.timestamp().isBefore(from) && !ohlcv.timestamp().isAfter(to))
                        .toList());
    }

    @Override
    public CompletableFuture<OHLCV> fetchLatest(String symbol) {
        String key = configuredApiKey != null && !configuredApiKey.isBlank() ? configuredApiKey : apiKey;
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/query")
                        .queryParam("function", "GLOBAL_QUOTE")
                        .queryParam("symbol", symbol)
                        .queryParam("apikey", key)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(this::isRateLimitOrServerError))
                .timeout(Duration.ofSeconds(30))
                .flatMap(this::parseGlobalQuote)
                .toFuture();
    }

    @Override
    public CompletableFuture<List<OHLCV>> fetchDaily(String symbol, int days) {
        String key = configuredApiKey != null && !configuredApiKey.isBlank() ? configuredApiKey : apiKey;
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/query")
                        .queryParam("function", "TIME_SERIES_DAILY")
                        .queryParam("symbol", symbol)
                        .queryParam("outputsize", days > 100 ? "full" : "compact")
                        .queryParam("apikey", key)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .delayElement(RATE_LIMIT_DELAY)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(this::isRateLimitOrServerError))
                .timeout(Duration.ofSeconds(60))
                .flatMap(this::parseTimeSeriesDaily)
                .map(list -> {
                    if (list.size() > days) {
                        return list.subList(0, days);
                    }
                    return list;
                })
                .map(Collections::unmodifiableList)
                .toFuture();
    }

    @Override
    public String getSource() {
        return "ALPHA_VANTAGE";
    }

     private Mono<List<OHLCV>> parseTimeSeriesDaily(String json) {
         return Mono.fromCallable(() -> {
             List<OHLCV> result = new ArrayList<>();
             try {
                 JsonNode root = objectMapper.readTree(json);
                 
                 JsonNode timeSeries = root.path("Time Series (Daily)");
                 if (timeSeries.isMissingNode()) {
                     String note = root.path("Note").asText("");
                     log.warn("Alpha Vantage response error: {}", note);
                     if (note.contains("API call frequency")) {
                         return result;
                     }
                     return result;
                 }
                 
                 timeSeries.fields().forEachRemaining(entry -> {
                     String dateStr = entry.getKey();
                     JsonNode data = entry.getValue();
                     
                     try {
                         Instant timestamp = LocalDate.parse(dateStr, DATE_FORMATTER)
                                 .atStartOfDay()
                                 .toInstant(ZoneOffset.UTC);
                         
                             OHLCV ohlcv = new OHLCV(
                                     new BigDecimal(data.path("1. open").asText()),
                                     new BigDecimal(data.path("2. high").asText()),
                                     new BigDecimal(data.path("3. low").asText()),
                                     new BigDecimal(data.path("4. close").asText()),
                                     new BigDecimal(data.path("5. volume").asText()),
                                     timestamp
                             );
                             result.add(ohlcv);
                         } catch (Exception e) {
                             log.warn("Failed to parse OHLCV for date {}: {}", dateStr, e.getMessage());
                         }
                     });
             } catch (RuntimeException e) {
                 throw e;
             } catch (Exception e) {
                 log.warn("Failed to parse Alpha Vantage response: {}", e.getMessage());
             }
             return result;
         });
      }

    private Mono<OHLCV> parseGlobalQuote(String json) {
        return Mono.fromCallable(() -> {
            JsonNode root = objectMapper.readTree(json);
            JsonNode quote = root.path("Global Quote");
            
            if (quote.isMissingNode() || quote.path("01. symbol").isMissingNode()) {
                String note = root.path("Note").asText("");
                if (note.contains("rate limit")) {
                    throw new RuntimeException("Alpha Vantage rate limit exceeded");
                }
                log.warn("Unexpected Global Quote response: {}", json);
                throw new RuntimeException("Invalid response from Alpha Vantage");
            }
            
            String dateStr = quote.path("07. latest trading day").asText();
            Instant timestamp = LocalDate.parse(dateStr, DATE_FORMATTER)
                    .atStartOfDay()
                    .toInstant(ZoneOffset.UTC);
            
            return new OHLCV(
                    new BigDecimal(quote.path("02. open").asText()),
                    new BigDecimal(quote.path("03. high").asText()),
                    new BigDecimal(quote.path("04. low").asText()),
                    new BigDecimal(quote.path("05. price").asText()),
                    new BigDecimal(quote.path("06. volume").asText()),
                    timestamp
            );
        });
    }

    private boolean isRateLimitOrServerError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException wcre) {
            int status = wcre.getStatusCode().value();
            return status == 429 || status >= 500;
        }
        return throwable.getMessage() != null && 
               throwable.getMessage().contains("rate limit");
    }
}

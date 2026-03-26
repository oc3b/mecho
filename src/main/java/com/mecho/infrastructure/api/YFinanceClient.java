package com.mecho.infrastructure.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mecho.domain.model.OHLCV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
public class YFinanceClient implements MarketDataApi {

    private static final Logger log = LoggerFactory.getLogger(YFinanceClient.class);

    private final ObjectMapper objectMapper;

    public YFinanceClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getSource() {
        return "YFINANCE";
    }

    @Override
    public CompletableFuture<List<OHLCV>> fetchHistorical(String symbol, Instant from, Instant to) {
        try {
            long fromEpoch = from.getEpochSecond();
            long toEpoch = to.getEpochSecond();

            String url = String.format("https://query1.finance.yahoo.com/v8/finance/chart/%s?range=1mo&interval=1d&period1=%d&period2=%d",
                    symbol, fromEpoch, toEpoch);

            log.debug("YFinance fetching historical data for {} with period1={},period2={}",
                    symbol, fromEpoch, toEpoch);

            WebClient webClient = WebClient.builder().baseUrl("https://query1.finance.yahoo.com").build();

            return webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(Retry.backoff(3, java.time.Duration.ofSeconds(1)))
                    .flatMap(response -> parseChartResponse(response, symbol))
                    .toFuture();

        } catch (Exception e) {
            log.error("YFinance fetchHistorical failed for {}: {}", symbol, e.getMessage());
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
    }

    @Override
    public CompletableFuture<OHLCV> fetchLatest(String symbol) {
        String url = String.format("https://query1.finance.yahoo.com/v8/finance/quote/%s?fields=regularMarketTime,regularMarketPrice,regularMarketOpen,regularMarketDayHigh,regularMarketDayLow,regularMarketVolume", symbol);

        WebClient webClient = WebClient.builder().baseUrl("https://query1.finance.yahoo.com").build();

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, java.time.Duration.ofSeconds(1)))
                .flatMap(this::parseQuoteResponse)
                .toFuture();
    }

    @Override
    public CompletableFuture<List<OHLCV>> fetchDaily(String symbol, int days) {
        String range = days > 60 ? "2mo" : days > 30 ? "1mo" : "5d";

        long fromEpoch = Instant.now().minus(days, java.time.temporal.ChronoUnit.DAYS).getEpochSecond();
        long toEpoch = Instant.now().getEpochSecond();

        String url = String.format("https://query1.finance.yahoo.com/v8/finance/chart/%s?range=%s&interval=1d&period1=%d&period2=%d",
                symbol, range, fromEpoch, toEpoch);

        try {
            WebClient webClient = WebClient.builder().baseUrl("https://query1.finance.yahoo.com").build();

            return webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(Retry.backoff(3, java.time.Duration.ofSeconds(1)))
                    .flatMap(response -> parseChartResponse(response, symbol))
                    .toFuture();
        } catch (Exception e) {
            log.error("YFinance fetchDaily failed for {}: {}", symbol, e.getMessage());
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
    }

    private Mono<List<OHLCV>> parseChartResponse(String json, String symbol) {
        return Mono.fromCallable(() -> {
            List<OHLCV> result = new ArrayList<>();

            try {
                JsonNode root = objectMapper.readTree(json);
                JsonNode chart = root.path("chart");

                if (chart.isMissingNode()) {
                    log.warn("YFinance response missing 'chart' node for {}", symbol);
                    return result;
                }

                JsonNode resultNode = chart.path("result");
                if (resultNode.isArray() && resultNode.size() > 0) {
                    JsonNode series = resultNode.get(0);

                    JsonNode timestamps = series.path("timestamp");
                    JsonNode quotes = series.path("indicators").path("quote").get(0);

                    if (quotes.isMissingNode()) {
                        log.warn("YFinance response missing 'quotes' node for {}", symbol);
                        return result;
                    }

                    JsonNode opens = quotes.path("open");
                    JsonNode highs = quotes.path("high");
                    JsonNode lows = quotes.path("low");
                    JsonNode closes = quotes.path("close");
                    JsonNode volumes = quotes.path("volume");

                    int count = timestamps.size();
                    for (int i = 0; i < count; i++) {
                        try {
                            long ts = timestamps.get(i).asLong(0);
                            if (ts == 0) continue;

                            BigDecimal open = opens.get(i) != null && !opens.get(i).isNull()
                                    ? new BigDecimal(opens.get(i).asDouble()) : null;
                            BigDecimal high = highs.get(i) != null && !highs.get(i).isNull()
                                    ? new BigDecimal(highs.get(i).asDouble()) : null;
                            BigDecimal low = lows.get(i) != null && !lows.get(i).isNull()
                                    ? new BigDecimal(lows.get(i).asDouble()) : null;
                            BigDecimal close = closes.get(i) != null && !closes.get(i).isNull()
                                    ? new BigDecimal(closes.get(i).asDouble()) : null;
                            BigDecimal volume = volumes.get(i) != null && !volumes.get(i).isNull()
                                    ? new BigDecimal(volumes.get(i).asLong()) : BigDecimal.ZERO;

                            if (close != null) {
                                Instant timestamp = Instant.ofEpochSecond(ts);
                                OHLCV ohlcv = new OHLCV(open, high, low, close, volume, timestamp);
                                result.add(ohlcv);
                            }
                        } catch (Exception e) {
                            log.debug("Failed to parse OHLCV at index {} for {}: {}", i, symbol, e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse YFinance chart response for {}: {}", symbol, e.getMessage());
            }

            return result;
        });
    }

    private Mono<OHLCV> parseQuoteResponse(String json) {
        return Mono.fromCallable(() -> {
            JsonNode root = objectMapper.readTree(json);
            JsonNode quoteResponse = root.path("quoteResponse");

            if (quoteResponse.isMissingNode()) {
                throw new RuntimeException("Invalid quote response - missing root node");
            }

            JsonNode resultArray = quoteResponse.path("result");
            if (resultArray.isArray() && resultArray.size() > 0) {
                JsonNode quote = resultArray.get(0);

                long ts = quote.path("regularMarketTime").asLong(0);
                BigDecimal price = new BigDecimal(quote.path("regularMarketPrice").asDouble());
                BigDecimal open = new BigDecimal(quote.path("regularMarketOpen").asDouble());
                BigDecimal high = new BigDecimal(quote.path("regularMarketDayHigh").asDouble());
                BigDecimal low = new BigDecimal(quote.path("regularMarketDayLow").asDouble());
                BigDecimal volume = new BigDecimal(quote.path("regularMarketVolume").asLong(0));

                Instant timestamp = ts > 0 ? Instant.ofEpochSecond(ts) : Instant.now();
                return new OHLCV(open, high, low, price, volume, timestamp);
            }

            throw new RuntimeException("No quote data found");
        });
    }
}

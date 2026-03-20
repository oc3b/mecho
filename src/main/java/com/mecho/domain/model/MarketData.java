package com.mecho.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record MarketData(
    Long id,
    Symbol symbol,
    Instant timestamp,
    BigDecimal openPrice,
    BigDecimal highPrice,
    BigDecimal lowPrice,
    BigDecimal closePrice,
    BigDecimal volume,
    String dataSource
) {
    
    public MarketData {
        Objects.requireNonNull(openPrice, "openPrice cannot be null");
        Objects.requireNonNull(highPrice, "highPrice cannot be null");
        Objects.requireNonNull(lowPrice, "lowPrice cannot be null");
        Objects.requireNonNull(closePrice, "closePrice cannot be null");
        Objects.requireNonNull(volume, "volume cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        if (dataSource == null) dataSource = "UNKNOWN";
    }

    public BigDecimal getTypicalPrice() {
        return highPrice.add(lowPrice).add(closePrice).divide(BigDecimal.valueOf(3), 8, java.math.RoundingMode.HALF_UP);
    }

    public BigDecimal getPriceRange() {
        return highPrice.subtract(lowPrice);
    }

    public static MarketData fromOHLCV(Symbol symbol, OHLCV ohlcv, String dataSource) {
        return new MarketData(
            null,
            symbol,
            ohlcv.timestamp(),
            ohlcv.open(),
            ohlcv.high(),
            ohlcv.low(),
            ohlcv.close(),
            ohlcv.volume(),
            dataSource
        );
    }
}

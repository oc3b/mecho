package com.mecho.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record OHLCV(
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,
    BigDecimal volume,
    Instant timestamp
) {
    public OHLCV {
        if (open == null) open = BigDecimal.ZERO;
        if (high == null) high = BigDecimal.ZERO;
        if (low == null) low = BigDecimal.ZERO;
        if (close == null) close = BigDecimal.ZERO;
        if (volume == null) volume = BigDecimal.ZERO;
        if (timestamp == null) timestamp = Instant.now();
    }

    public BigDecimal getTypicalPrice() {
        return high.add(low).add(close).divide(BigDecimal.valueOf(3), 8, java.math.RoundingMode.HALF_UP);
    }

    public BigDecimal getPriceRange() {
        return high.subtract(low);
    }
}

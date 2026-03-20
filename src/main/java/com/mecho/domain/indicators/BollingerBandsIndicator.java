package com.mecho.domain.indicators;

import com.mecho.domain.model.OHLCV;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class BollingerBandsIndicator implements Indicator {
    
    private static final String CODE = "BB";
    private static final int PERIOD = 20;
    private static final int STD_DEV_MULTIPLIER = 2;
    
    @Override
    public String getCode() {
        return CODE;
    }
    
    @Override
    public double calculate(List<OHLCV> prices) {
        if (prices == null || prices.size() < PERIOD) {
            return 0.5;
        }
        
        List<BigDecimal> closes = prices.subList(prices.size() - PERIOD, prices.size())
            .stream()
            .map(OHLCV::close)
            .toList();
        
        BigDecimal sma = closes.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(PERIOD), 8, RoundingMode.HALF_UP);
        
        BigDecimal sumSquaredDiff = BigDecimal.ZERO;
        for (BigDecimal price : closes) {
            BigDecimal diff = price.subtract(sma);
            sumSquaredDiff = sumSquaredDiff.add(diff.multiply(diff));
        }
        
        BigDecimal variance = sumSquaredDiff.divide(BigDecimal.valueOf(PERIOD), 8, RoundingMode.HALF_UP);
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
        
        BigDecimal lowerBand = sma.subtract(stdDev.multiply(BigDecimal.valueOf(STD_DEV_MULTIPLIER)));
        BigDecimal upperBand = sma.add(stdDev.multiply(BigDecimal.valueOf(STD_DEV_MULTIPLIER)));
        
        BigDecimal currentClose = closes.get(closes.size() - 1);
        BigDecimal bandWidth = upperBand.subtract(lowerBand);
        
        if (bandWidth.compareTo(BigDecimal.ZERO) == 0) {
            return 0.5;
        }
        
        BigDecimal position = currentClose.subtract(lowerBand)
            .divide(bandWidth, 8, RoundingMode.HALF_UP);
        
        return position.doubleValue();
    }
    
    @Override
    public Signal calculateSignal(double value) {
        if (value < 0.2) {
            return Signal.BUY;
        } else if (value > 0.8) {
            return Signal.SELL;
        }
        return Signal.NEUTRAL;
    }
}

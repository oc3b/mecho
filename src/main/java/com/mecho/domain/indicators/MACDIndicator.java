package com.mecho.domain.indicators;

import com.mecho.domain.model.OHLCV;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class MACDIndicator implements Indicator {
    
    private static final String CODE = "MACD";
    private static final int FAST_EMA = 12;
    private static final int SLOW_EMA = 26;
    private static final int SIGNAL_EMA = 9;
    
    @Override
    public String getCode() {
        return CODE;
    }
    
    @Override
    public double calculate(List<OHLCV> prices) {
        if (prices == null || prices.size() < SLOW_EMA + SIGNAL_EMA) {
            return 0.0;
        }
        
        List<BigDecimal> closes = prices.stream()
            .map(OHLCV::close)
            .toList();
        
        List<BigDecimal> emaFast = calculateEMA(closes, FAST_EMA);
        List<BigDecimal> emaSlow = calculateEMA(closes, SLOW_EMA);
        
        if (emaFast.isEmpty() || emaSlow.isEmpty()) {
            return 0.0;
        }
        
        List<BigDecimal> macdLine = new ArrayList<>();
        
        int startIdx = emaFast.size() - emaSlow.size();
        for (int i = 0; i < emaSlow.size(); i++) {
            macdLine.add(emaFast.get(startIdx + i).subtract(emaSlow.get(i)));
        }
        
        if (macdLine.size() < SIGNAL_EMA) {
            return 0.0;
        }
        
        List<BigDecimal> signalLine = calculateEMA(macdLine, SIGNAL_EMA);
        
        if (signalLine.isEmpty()) {
            return 0.0;
        }
        
        BigDecimal macdValue = macdLine.get(macdLine.size() - 1);
        double result = macdValue.doubleValue();
        
        // Round to 10 decimal places to handle floating point precision
        return Math.round(result * 1e10) / 1e10;
    }
    
    @Override
    public Signal calculateSignal(double value) {
        if (value > 0) {
            return Signal.BUY;
        } else if (value < 0) {
            return Signal.SELL;
        }
        return Signal.NEUTRAL;
    }
    
    private List<BigDecimal> calculateEMA(List<BigDecimal> prices, int period) {
        if (prices.size() < period) {
            return new ArrayList<>();
        }
        
        List<BigDecimal> ema = new ArrayList<>();
        double multiplier = 2.0 / (period + 1);
        
        BigDecimal initialSMA = prices.subList(0, period).stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
        ema.add(initialSMA);
        
        for (int i = period; i < prices.size(); i++) {
            BigDecimal previousEMA = ema.get(ema.size() - 1);
            BigDecimal currentPrice = prices.get(i);
            BigDecimal currentEMA = currentPrice.multiply(BigDecimal.valueOf(multiplier))
                .add(previousEMA.multiply(BigDecimal.valueOf(1 - multiplier)));
            ema.add(currentEMA);
        }
        
        return ema;
    }
}

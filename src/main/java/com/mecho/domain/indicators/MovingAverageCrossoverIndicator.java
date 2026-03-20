package com.mecho.domain.indicators;

import com.mecho.domain.model.OHLCV;
import java.math.BigDecimal;
import java.util.List;

public class MovingAverageCrossoverIndicator implements Indicator {
    
    private static final String CODE = "MACROSS";
    private static final int FAST_MA = 50;
    private static final int SLOW_MA = 200;
    
    @Override
    public String getCode() {
        return CODE;
    }
    
    @Override
    public double calculate(List<OHLCV> prices) {
        if (prices == null || prices.size() < SLOW_MA + 1) {
            return 0.0;
        }
        
        List<BigDecimal> closes = prices.stream()
            .map(OHLCV::close)
            .toList();
        
        double fastMA = calculateSMA(closes.subList(closes.size() - FAST_MA, closes.size()));
        double slowMA = calculateSMA(closes.subList(closes.size() - SLOW_MA, closes.size()));
        
        double prevFastMA = calculateSMA(closes.subList(closes.size() - FAST_MA - 1, closes.size() - 1));
        double prevSlowMA = calculateSMA(closes.subList(closes.size() - SLOW_MA - 1, closes.size() - 1));
        
        boolean currentBullish = fastMA > slowMA;
        boolean previousBullish = prevFastMA > prevSlowMA;
        
        if (fastMA == slowMA && prevFastMA == prevSlowMA) {
            return 0.0;
        }
        
        if (currentBullish && !previousBullish) {
            return 2.0;
        } else if (!currentBullish && previousBullish) {
            return -2.0;
        } else if (currentBullish) {
            return 1.0;
        } else if (!currentBullish) {
            return -1.0;
        }
        return 0.0;
    }
    
    @Override
    public Signal calculateSignal(double value) {
        if (value >= 1.0) {
            return Signal.BUY;
        } else if (value <= -1.0) {
            return Signal.SELL;
        }
        return Signal.NEUTRAL;
    }
    
    private double calculateSMA(List<BigDecimal> prices) {
        return prices.stream()
            .map(BigDecimal::doubleValue)
            .reduce(0.0, Double::sum) / prices.size();
    }
}

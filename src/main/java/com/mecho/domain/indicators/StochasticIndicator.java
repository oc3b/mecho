package com.mecho.domain.indicators;

import com.mecho.domain.model.OHLCV;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class StochasticIndicator implements Indicator {
    
    private static final String CODE = "STOCH";
    private static final int PERIOD = 14;
    private static final int SMOOTH_K = 3;
    private static final double OVERBOUGHT = 80.0;
    private static final double OVERSOLD = 20.0;
    
    @Override
    public String getCode() {
        return CODE;
    }
    
    @Override
    public double calculate(List<OHLCV> prices) {
        if (prices == null || prices.size() < PERIOD + SMOOTH_K - 1) {
            return 50.0;
        }
        
        java.util.List<Double> kValues = new java.util.ArrayList<>();
        
        for (int i = PERIOD - 1; i < prices.size(); i++) {
            BigDecimal lowestLow = prices.get(i - PERIOD + 1).low();
            BigDecimal highestHigh = prices.get(i - PERIOD + 1).high();
            
            for (int j = i - PERIOD + 2; j <= i; j++) {
                if (prices.get(j).low().compareTo(lowestLow) < 0) {
                    lowestLow = prices.get(j).low();
                }
                if (prices.get(j).high().compareTo(highestHigh) > 0) {
                    highestHigh = prices.get(j).high();
                }
            }
            
            BigDecimal close = prices.get(i).close();
            BigDecimal range = highestHigh.subtract(lowestLow);
            
            double k;
            if (range.compareTo(BigDecimal.ZERO) == 0) {
                k = 50.0;
            } else {
                k = close.subtract(lowestLow)
                    .divide(range, 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
                k = Math.max(0.0, Math.min(100.0, k));
            }
            kValues.add(k);
        }
        
        if (kValues.size() < SMOOTH_K) {
            return kValues.isEmpty() ? 50.0 : kValues.get(kValues.size() - 1);
        }
        
        double smoothedK = 0;
        for (int i = kValues.size() - SMOOTH_K; i < kValues.size(); i++) {
            smoothedK += kValues.get(i);
        }
        smoothedK /= SMOOTH_K;
        
        return smoothedK;
    }
    
    @Override
    public Signal calculateSignal(double value) {
        if (value < OVERSOLD) {
            return Signal.BUY;
        } else if (value > OVERBOUGHT) {
            return Signal.SELL;
        }
        return Signal.NEUTRAL;
    }
}

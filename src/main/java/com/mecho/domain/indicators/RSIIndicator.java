package com.mecho.domain.indicators;

import com.mecho.domain.model.OHLCV;
import java.math.BigDecimal;
import java.util.List;

public class RSIIndicator implements Indicator {
    
    private static final String CODE = "RSI";
    private static final int PERIOD = 14;
    private static final double OVERBOUGHT = 70.0;
    private static final double OVERSOLD = 30.0;
    
    @Override
    public String getCode() {
        return CODE;
    }
    
    @Override
    public double calculate(List<OHLCV> prices) {
        if (prices == null || prices.size() < PERIOD + 1) {
            return 50.0;
        }
        
        BigDecimal avgGain = BigDecimal.ZERO;
        BigDecimal avgLoss = BigDecimal.ZERO;
        
        for (int i = prices.size() - PERIOD; i < prices.size(); i++) {
            BigDecimal change = prices.get(i).close().subtract(prices.get(i - 1).close());
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                avgGain = avgGain.add(change);
            } else {
                avgLoss = avgLoss.add(change.abs());
            }
        }
        
        avgGain = avgGain.divide(BigDecimal.valueOf(PERIOD), 8, BigDecimal.ROUND_HALF_UP);
        avgLoss = avgLoss.divide(BigDecimal.valueOf(PERIOD), 8, BigDecimal.ROUND_HALF_UP);
        
        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) {
            if (avgGain.compareTo(BigDecimal.ZERO) == 0) {
                return 50.0;
            }
            return 100.0;
        }
        
        double rs = avgGain.divide(avgLoss, 8, BigDecimal.ROUND_HALF_UP).doubleValue();
        return 100.0 - (100.0 / (1.0 + rs));
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

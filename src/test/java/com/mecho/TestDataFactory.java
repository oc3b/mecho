package com.mecho;

import com.mecho.domain.model.OHLCV;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public final class TestDataFactory {
    
    private TestDataFactory() {}
    
    public static OHLCV createOHLCV(BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, BigDecimal volume) {
        return new OHLCV(open, high, low, close, volume, Instant.now());
    }
    
    public static OHLCV createOHLCV(double open, double high, double low, double close, double volume) {
        return createOHLCV(
            BigDecimal.valueOf(open),
            BigDecimal.valueOf(high),
            BigDecimal.valueOf(low),
            BigDecimal.valueOf(close),
            BigDecimal.valueOf(volume)
        );
    }
    
    public static List<OHLCV> createPriceList(double... prices) {
        List<OHLCV> result = new ArrayList<>();
        for (int i = 0; i < prices.length; i++) {
            double price = prices[i];
            result.add(createOHLCV(price, price + 0.5, price - 0.5, price, 1000));
        }
        return result;
    }
    
    public static List<OHLCV> createIncreasingPriceList(int count, double startPrice, double increment) {
        List<OHLCV> result = new ArrayList<>();
        double currentPrice = startPrice;
        Instant timestamp = Instant.now().minus(count, ChronoUnit.DAYS);
        
        for (int i = 0; i < count; i++) {
            result.add(createOHLCV(
                currentPrice,
                currentPrice + increment * 0.3,
                currentPrice - increment * 0.2,
                currentPrice + increment * 0.5,
                1000000
            ));
            currentPrice += increment;
            timestamp = timestamp.plus(1, ChronoUnit.DAYS);
        }
        return result;
    }
    
    public static List<OHLCV> createDecreasingPriceList(int count, double startPrice, double decrement) {
        List<OHLCV> result = new ArrayList<>();
        double currentPrice = startPrice;
        
        for (int i = 0; i < count; i++) {
            result.add(createOHLCV(
                currentPrice,
                currentPrice + decrement * 0.2,
                currentPrice - decrement * 0.3,
                currentPrice - decrement * 0.5,
                1000000
            ));
            currentPrice -= decrement;
        }
        return result;
    }
    
    public static List<OHLCV> createVolatilePriceList(int count, double basePrice) {
        List<OHLCV> result = new ArrayList<>();
        double currentPrice = basePrice;
        
        for (int i = 0; i < count; i++) {
            double change = (Math.random() - 0.5) * basePrice * 0.1;
            currentPrice = basePrice + change;
            result.add(createOHLCV(
                currentPrice,
                currentPrice + Math.abs(change) + 0.5,
                currentPrice - Math.abs(change) - 0.5,
                currentPrice + (Math.random() - 0.5) * 0.5,
                1000000
            ));
        }
        return result;
    }
    
    public static List<OHLCV> createFlatPriceList(int count, double price) {
        List<OHLCV> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(createOHLCV(price, price, price, price, 1000));
        }
        return result;
    }
    
    public static List<OHLCV> createRSITestData() {
        List<OHLCV> result = new ArrayList<>();
        double[] gains = {0, 0, 0, 2.0, 0, 0, 0, 1.5, 0, 0, 3.0, 0, 0, 0, 2.5, 0, 0, 1.0, 0, 0};
        double basePrice = 100.0;
        
        for (int i = 0; i < gains.length; i++) {
            double change = gains[i];
            double close = basePrice + change;
            result.add(createOHLCV(basePrice, close + 0.2, basePrice - 0.1, close, 1000));
            basePrice = close;
        }
        return result;
    }
    
    public static List<OHLCV> createMACDTestData() {
        return createIncreasingPriceList(50, 100.0, 0.5);
    }
    
    public static List<OHLCV> createBollingerTestData() {
        return createVolatilePriceList(30, 100.0);
    }
    
    public static List<OHLCV> createStochasticTestData() {
        List<OHLCV> result = new ArrayList<>();
        double[] closes = {95, 100, 98, 102, 105, 108, 103, 100, 97, 104, 107, 110, 106, 102};
        
        for (int i = 0; i < closes.length; i++) {
            double close = closes[i];
            double low = close - 2;
            double high = close + 3;
            result.add(createOHLCV(close - 0.5, high, low, close, 1000));
        }
        return result;
    }
    
    public static List<OHLCV> createMovingAverageTestData() {
        List<OHLCV> result = new ArrayList<>();
        
        for (int i = 0; i < 250; i++) {
            double trend = i < 150 ? 100.0 + i * 0.1 : 115.0 - (i - 150) * 0.1;
            double noise = (Math.random() - 0.5) * 2;
            result.add(createOHLCV(trend + noise, trend + 1, trend - 1, trend + noise, 1000));
        }
        return result;
    }
}

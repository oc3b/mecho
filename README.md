# Mecho - Market Analysis System

A comprehensive market data fetching, technical analysis, and alerting system for stocks and cryptocurrencies.

## Overview

Mecho automatically fetches market data, analyzes it using multiple technical indicators, calculates prediction probabilities, and sends alerts via Telegram when trading opportunities are detected.

### Features

- **Multi-Asset Support**: US stocks, European stocks, and cryptocurrencies
- **5 Technical Indicators**: RSI, MACD, Bollinger Bands, Moving Average Crossover, Stochastic Oscillator
- **Automated Alerts**: Telegram notifications when prediction probability exceeds threshold (default 70%)
- **Scheduled Analysis**: Automatic market data fetching and analysis every 15 minutes
- **H2 Database**: Local persistence for market data, predictions, and alerts

### Tech Stack

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 4.0 |
| Language | Java 21 |
| Build Tool | Maven |
| Database | H2 (in-memory) |
| Migrations | Liquibase |
| HTTP Client | WebFlux |
| Testing | JUnit 5, Mockito, AssertJ |

---

## Prerequisites

Before running Mecho, ensure you have:

- **Java 21** or higher
- **Maven 3.9** or higher
- **AlphaVantage API Key** (free at [alphavantage.co](https://www.alphavantage.co/support/#api-key))
- **Telegram Bot Token** (created via @BotFather)

---

## Configuration Guide

### 1. Get AlphaVantage API Key

1. Visit [https://www.alphavantage.co/support/#api-key](https://www.alphavantage.co/support/#api-key)
2. Enter your email and complete the form
3. Copy your API key (e.g., `XSFT9QGVX7PUVVF5`)
4. Free tier allows 25 API calls per day

### 2. Create Telegram Bot

1. Open Telegram and search for **@BotFather**
2. Send `/newbot`
3. Follow the prompts (choose name and username)
4. Copy the bot token (e.g., `8730848328:AAFTu0bmm3b4EHvcgF84zMdV2YMp3sdW7Wg`)
5. Start a chat with your bot and send any message
6. Get your Chat ID using `@userinfobot` or the API:
   ```
   https://api.telegram.org/bot<YOUR_TOKEN>/getUpdates
   ```

### 3. Configure Environment Variables

Set these environment variables before running:

```bash
export ALPHA_VANTAGE_API_KEY="your_api_key_here"
export TELEGRAM_BOT_TOKEN="your_telegram_token_here"
export TELEGRAM_CHANNEL_ID="your_chat_id_here"
```

### 4. Configuration Files

All configuration files are located in the `config/` directory.

#### config/symbols.yml

Defines which assets to analyze:

```yaml
symbols:
  # US Stocks (Yahoo Finance format)
  us:
    - AAPL      # Apple
    - MSFT      # Microsoft
    - GOOGL     # Google
    - AMZN      # Amazon
    - TSLA      # Tesla
    - NVDA      # NVIDIA
    - META      # Meta
    - NFLX      # Netflix
    - AMD       # AMD
    - JPM       # JPMorgan
  
  # European Stocks (Yahoo Finance format)
  eu:
    - SAP.DE    # SAP (Germany)
    - ASML.AS   # ASML (Netherlands)
    - SIE.DE    # Siemens (Germany)
    - NESN.SW   # Nestlé (Switzerland)
    - NOVO-B.CO # Novo Nordisk (Denmark)
  
  # Cryptocurrencies (CoinGecko IDs)
  crypto:
    - bitcoin
    - ethereum
    - solana
    - ripple
    - polkadot
    - cardano
```

#### config/providers.yml

Configures data providers and API settings:

```yaml
# Data Provider Configuration
providers:
  stocks:
    # Alpha Vantage (Free tier: 25 calls/day)
    alphaVantage:
      enabled: true
      apiKey: "${ALPHA_VANTAGE_API_KEY}"  # Set via environment variable
      baseUrl: "https://www.alphavantage.co/query"
      requestDelayMs: 1200  # Rate limit protection
  
  crypto:
    # CoinGecko (No API key for basic tier)
    coinGecko:
      enabled: true
      baseUrl: "https://api.coingecko.com/api/v3"
      requestDelayMs: 1000

# Fetch Intervals
frequency:
  fetchIntervalMinutes: 15
  historicalDays: 200
```

#### config/alerts.yml

Configures alert thresholds and Telegram settings:

```yaml
# Alert Configuration

# Minimum probability threshold to trigger alert (0.0 - 1.0)
# Set to 0.7 = 70% probability required
alertThreshold: 0.7

# Telegram notification settings
telegram:
  enabled: true
  botToken: "${TELEGRAM_BOT_TOKEN}"
  channelId: "${TELEGRAM_CHANNEL_ID}"

# Alert message template
alertTemplate: |
  📊 %s | Probability: %.0f%% | %s
  
  Indicators:
  %s
  
  Price: %.2f | Volume: %s
  Timestamp: %s
```

#### config/indicators.yaml

Defines technical indicators and their parameters:

```yaml
# RSI (Relative Strength Index)
---
name: RSI
code: rsi
type: momentum
parameters:
  period: 14
  overbought: 70
  oversold: 30
thresholds:
  signal_up: 30    # RSI below 30 = oversold = potential BUY
  signal_down: 70  # RSI above 70 = overbought = potential SELL
weight: 0.25

# MACD (Moving Average Convergence Divergence)
---
name: MACD
code: macd
type: trend
parameters:
  fastPeriod: 12
  slowPeriod: 26
  signalPeriod: 9
weight: 0.20

# Bollinger Bands
---
name: Bollinger
code: bollinger
type: volatility
parameters:
  period: 20
  stdDevMultiplier: 2.0
weight: 0.20

# Moving Average Crossover (Golden/Death Cross)
---
name: MovingAverageCrossover
code: ma_crossover
type: trend
parameters:
  shortPeriod: 50
  longPeriod: 200
weight: 0.20

# Stochastic Oscillator
---
name: Stochastic
code: stochastic
type: momentum
parameters:
  kPeriod: 14
  dPeriod: 3
  overbought: 80
  oversold: 20
weight: 0.15
```

---

## Installation

### Clone and Build

```bash
# Clone the repository
git clone https://github.com/yourusername/mecho.git
cd mecho

# Install dependencies and build
mvn clean package

# Run tests
mvn test
```

### Using Maven Wrapper

If Maven is not installed, the project can be built using the wrapper (if available):

```bash
./mvnw clean package
./mvnw test
```

---

## Running the Application

### Start Mecho

```bash
# Set environment variables
export ALPHA_VANTAGE_API_KEY="your_api_key"
export TELEGRAM_BOT_TOKEN="your_bot_token"
export TELEGRAM_CHANNEL_ID="your_chat_id"

# Run with Maven
mvn spring-boot:run
```

### Expected Startup Logs

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v4.0.0)

2026-03-20 10:00:00.001  INFO 12345 --- [           main] com.mecho.MechoApplication       : Starting MechoApplication
2026-03-20 10:00:00.123  INFO 12345 --- [           main] com.mecho.MechoApplication       : Started MechoApplication in 2.5 seconds
2026-03-20 10:00:00.456  INFO 12345 --- [   scheduled] com.mecho.scheduler.MarketDataScheduler : Starting market analysis pipeline
2026-03-20 10:00:01.234  INFO 12345 --- [           main] com.mecho.MechoApplication       : Market Analysis System ready!
```

### Scheduled Execution

The analysis pipeline runs automatically every 15 minutes (configurable):

| Task | Schedule |
|------|----------|
| Fetch Market Data | Every 15 minutes |
| Analyze Indicators | After data fetch |
| Send Alerts | If probability > threshold |

---

## How It Works

### Data Flow

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Alpha      │    │    Fetch     │    │   Analyze    │    │    Alert     │
│  Vantage /   │───▶│   Market     │───▶│  Indicators  │───▶│  Notification│
│  CoinGecko   │    │    Data      │    │   (5 types)  │    │   (Telegram) │
└──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
```

### 1. Data Fetching

- **Stocks**: AlphaVantage API (25 calls/day on free tier)
- **Crypto**: CoinGecko API (no API key required)
- Historical data: 100+ days for accurate calculations
- Rate limit protection built-in

### 2. Technical Analysis

Five indicators analyze each symbol:

| Indicator | Type | Weight | Signal |
|-----------|------|--------|--------|
| RSI | Momentum | 25% | Overbought/Oversold |
| MACD | Trend | 20% | Crossover |
| Bollinger Bands | Volatility | 20% | Price extremes |
| MA Crossover | Trend | 20% | Golden/Death cross |
| Stochastic | Momentum | 15% | %K/%D crossover |

### 3. Prediction Calculation

The `WeightedPredictionCalculator` combines all indicator signals:

1. Each indicator generates BUY (+1.0), SELL (-1.0), or NEUTRAL (0.0)
2. Weighted sum is calculated based on configured weights
3. Normalized to probability (0.0 - 1.0)
4. Direction determined by sign of score

### 4. Alert Generation

Alerts are sent when:
- Probability >= threshold (default 70%)
- Direction is UP or DOWN (not NEUTRAL)

### Alert Format Example

```
📊 TRADING ALERT*

*AAPL* 💰 BULLISH
Probability: 78%
Confidence: 75%
Price: $182.45
Volume: 52.34M

Timestamp: 2026-03-20 10:15:00
```

---

## Configuration Files Reference

### symbols.yml

| Section | Description |
|---------|-------------|
| `us` | US stock tickers (Yahoo format) |
| `eu` | European stock tickers (with exchange suffix) |
| `crypto` | Cryptocurrency IDs (CoinGecko format) |

### providers.yml

| Setting | Description |
|---------|-------------|
| `enabled` | Enable/disable provider |
| `apiKey` | API authentication key |
| `requestDelayMs` | Delay between requests (rate limit) |
| `fetchIntervalMinutes` | How often to fetch data |
| `historicalDays` | Days of historical data to load |

### alerts.yml

| Setting | Description |
|---------|-------------|
| `alertThreshold` | Minimum probability (0.0-1.0) |
| `telegram.enabled` | Enable Telegram alerts |
| `telegram.botToken` | Bot authentication token |
| `telegram.channelId` | Target chat/channel ID |

### indicators.yaml

| Parameter | Description |
|-----------|-------------|
| `code` | Unique identifier |
| `type` | Category (momentum/trend/volatility) |
| `parameters.*` | Indicator-specific settings |
| `weight` | Contribution to final prediction (sum to 1.0) |

---

## Customization

### Add New Symbols

Edit `config/symbols.yml`:

```yaml
symbols:
  us:
    - AAPL
    - MSFT
    - NEW_SYMBOL  # Add your symbol here
  crypto:
    - bitcoin
    - new-coin    # Add crypto by CoinGecko ID
```

### Add New Indicator

1. Create indicator class implementing `Indicator`:

```java
package com.mecho.domain.indicators;

import com.mecho.domain.model.OHLCV;
import java.util.List;

public class MyCustomIndicator implements Indicator {
    
    @Override
    public String getCode() {
        return "MY_CUSTOM";
    }
    
    @Override
    public double calculate(List<OHLCV> prices) {
        // Your calculation logic
        return 50.0; // Return value
    }
    
    @Override
    public Signal calculateSignal(double value) {
        if (value > 60) return Signal.SELL;
        if (value < 40) return Signal.BUY;
        return Signal.NEUTRAL;
    }
}
```

2. Register as Spring bean:

```java
@Bean
public Indicator myCustomIndicator() {
    return new MyCustomIndicator();
}
```

3. Add configuration in `config/indicators.yaml`:

```yaml
---
name: MyCustom
code: MY_CUSTOM
type: custom
parameters:
  threshold: 50
weight: 0.10
```

### Modify Alert Threshold

Edit `config/alerts.yml`:

```yaml
alertThreshold: 0.8  # 80% probability required for alerts
```

Or set via environment variable:

```bash
export MECHO_ALERT_THRESHOLD=0.8
```

---

## Testing

### Run All Tests

```bash
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=RSIIndicatorTest
```

### Run with Coverage

```bash
mvn test jacoco:report
```

Coverage reports are generated in `target/site/jacoco/`.

### Test Structure

```
src/test/java/com/mecho/
├── domain/
│   ├── indicators/
│   │   ├── RSIIndicatorTest.java
│   │   ├── MACDIndicatorTest.java
│   │   ├── BollingerBandsIndicatorTest.java
│   │   ├── MovingAverageCrossoverIndicatorTest.java
│   │   └── StochasticIndicatorTest.java
│   └── prediction/
│       └── WeightedPredictionCalculatorTest.java
├── application/
│   ├── service/
│   │   ├── FetchMarketDataServiceTest.java
│   │   ├── AnalyzeIndicatorsServiceTest.java
│   │   └── AlertNotificationServiceTest.java
│   └── config/
│       ├── ConfigLoaderTest.java
│       └── IndicatorLoaderTest.java
└── infrastructure/
    └── telegram/
        └── TelegramBotClientTest.java
```

---

## Troubleshooting

### Common Issues

#### AlphaVantage Rate Limit

**Symptom**: `API call frequency exceeded`

**Solution**: 
- Wait 1-2 minutes between requests
- Increase `requestDelayMs` in `providers.yml`
- Upgrade to premium tier for more calls

#### Telegram Message Failed

**Symptom**: `Failed to send Telegram message`

**Solutions**:
1. Verify bot token is correct
2. Ensure bot has started a chat with the channel
3. Check channel ID is correct (include `-100` prefix for channels)
4. Verify bot has permission to send messages

#### Insufficient Data

**Symptom**: `Insufficient market data for symbol`

**Solution**: 
- Wait for more historical data to accumulate
- Reduce minimum data threshold in code
- Check API response contains valid data

#### No Alerts Triggering

**Symptom**: Alerts not being sent despite market movements

**Solutions**:
1. Lower `alertThreshold` in `alerts.yml` (e.g., 0.5 for 50%)
2. Check logs for prediction probabilities
3. Verify symbols are correctly configured
4. Ensure Telegram is enabled

### Debug Mode

Enable debug logging in `application.yml`:

```yaml
logging:
  level:
    com.mecho: DEBUG
    org.springframework: INFO
```

### API Rate Limits

| Provider | Free Tier Limit | Notes |
|----------|-----------------|-------|
| AlphaVantage | 25 calls/day | Stocks only |
| CoinGecko | 10-50 calls/min | Crypto, no key needed |

### Get Help

- Check logs for detailed error messages
- Enable debug mode for verbose output
- Verify all configuration files are valid YAML

---

## Project Structure

```
mecho/
├── config/                  # Configuration files
│   ├── symbols.yml          # Asset symbols
│   ├── providers.yml        # Data providers
│   ├── alerts.yml           # Alert settings
│   └── indicators.yaml      # Technical indicators
├── src/main/java/com/mecho/
│   ├── MechoApplication.java
│   ├── domain/
│   │   ├── indicators/      # Technical indicator implementations
│   │   ├── model/           # Domain models (OHLCV, Symbol, etc.)
│   │   ├── prediction/      # Prediction calculator
│   │   └── alert/           # Alert model
│   ├── application/
│   │   ├── config/          # Configuration loaders
│   │   ├── scheduler/       # Scheduled tasks
│   │   └── service/         # Business services
│   └── infrastructure/
│       ├── api/             # External API clients
│       ├── persistence/     # Database entities/repositories
│       └── telegram/         # Telegram bot client
├── src/main/resources/
│   ├── application.yml      # Spring configuration
│   └── db/                  # Liquibase migrations
└── src/test/               # Unit tests
```

---

## License

This project is for educational purposes. Market analysis and trading signals should not be used as the sole basis for investment decisions.

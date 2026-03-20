package com.mecho.application.service;

import com.mecho.application.config.ConfigLoader;
import com.mecho.application.config.TelegramConfig;
import com.mecho.domain.alert.Alert;
import com.mecho.domain.model.AlertStatus;
import com.mecho.domain.model.PredictionDirection;
import com.mecho.domain.prediction.Prediction;
import com.mecho.infrastructure.persistence.entities.AlertEntity;
import com.mecho.infrastructure.persistence.repositories.AlertRepository;
import com.mecho.infrastructure.telegram.TelegramBotClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class AlertNotificationService {
    
    private static final Logger log = LoggerFactory.getLogger(AlertNotificationService.class);
    private static final DateTimeFormatter DATE_FORMAT = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    
    private final TelegramBotClient telegramBotClient;
    private final AlertRepository alertRepository;
    private final TelegramConfig telegramConfig;
    private final ConfigLoader configLoader;
    private final Map<String, String> indicatorEmojis;
    
    public AlertNotificationService(
            TelegramBotClient telegramBotClient,
            AlertRepository alertRepository,
            TelegramConfig telegramConfig,
            ConfigLoader configLoader) {
        this.telegramBotClient = telegramBotClient;
        this.alertRepository = alertRepository;
        this.telegramConfig = telegramConfig;
        this.configLoader = configLoader;
        
        this.indicatorEmojis = new HashMap<>();
        this.indicatorEmojis.put("RSI", "\uD83D\uDCCA RSI");
        this.indicatorEmojis.put("MACD", "\uD83D\uDCCA MACD");
        this.indicatorEmojis.put("Bollinger", "\uD83D\uDCCA BB");
        this.indicatorEmojis.put("MA_Cross", "\uD83D\uDCCA MA");
        this.indicatorEmojis.put("Stochastic", "\uD83D\uDCCA STOCH");
    }
    
    @Transactional
    public Alert createAndSendAlert(Prediction prediction, BigDecimal currentPrice, Long volume) {
        double threshold = configLoader.getAlertThreshold();
        
        if (prediction.probability().doubleValue() < threshold) {
            log.debug("Prediction probability {} below threshold {}", 
                    prediction.probability(), threshold);
            return null;
        }
        
        if (prediction.direction() == PredictionDirection.NEUTRAL) {
            log.debug("Neutral prediction, skipping alert");
            return null;
        }
        
        Alert alert = Alert.create(prediction, "TELEGRAM");
        
        String formattedMessage = formatAlertMessage(prediction, currentPrice, volume);
        
        Alert savedAlert = saveAlert(alert);
        
        sendAlertAsync(savedAlert, formattedMessage);
        
        return savedAlert;
    }
    
    public String formatAlertMessage(Prediction prediction, BigDecimal currentPrice, Long volume) {
        String direction = prediction.direction() == PredictionDirection.UP ? "\uD83D\uDCB0 BULLISH" : "\uD83D\uDCB1 BEARISH";
        String probabilityPercent = String.format("%.0f%%", prediction.probability().doubleValue() * 100);
        
        StringBuilder message = new StringBuilder();
        message.append("\uD83D\uDCC8 *TRADING ALERT*\n\n");
        message.append(String.format("*%s* %s\n", prediction.symbol().getTicker(), direction));
        message.append(String.format("Probability: %s\n", probabilityPercent));
        
        if (prediction.confidence() != null) {
            String confidencePercent = String.format("%.0f%%", prediction.confidence().doubleValue() * 100);
            message.append(String.format("Confidence: %s\n", confidencePercent));
        }
        
        if (currentPrice != null) {
            message.append(String.format("Price: $%.2f\n", currentPrice.doubleValue()));
        }
        
        if (volume != null) {
            message.append(String.format("Volume: %s\n", formatVolume(volume)));
        }
        
        message.append(String.format("\nTimestamp: %s", DATE_FORMAT.format(Instant.now())));
        
        return message.toString();
    }
    
    public String formatSimpleAlertMessage(Prediction prediction) {
        String direction = prediction.direction() == PredictionDirection.UP ? "\uD83D\uDCB0 UP" : "\uD83D\uDCB1 DOWN";
        String probabilityPercent = String.format("%.0f%%", prediction.probability().doubleValue() * 100);
        
        return String.format("\uD83D\uDCC8 %s | %s | Probability: %s",
                prediction.symbol().getTicker(),
                direction,
                probabilityPercent);
    }
    
    @Transactional
    protected Alert saveAlert(Alert alert) {
        AlertEntity entity = AlertEntity.builder()
                .channel(AlertEntity.Channel.TELEGRAM)
                .status(AlertEntity.AlertStatus.PENDING)
                .sentAt(alert.sentAt())
                .build();
        
        AlertEntity saved = alertRepository.save(entity);
        
        return new Alert(saved.getId(), alert.prediction(), saved.getSentAt(), 
                saved.getChannel().name(), toAlertStatus(saved.getStatus()));
    }
    
    protected void sendAlertAsync(Alert alert, String message) {
        if (!telegramConfig.isEnabled()) {
            log.info("Telegram notifications disabled, skipping alert");
            return;
        }
        
        String channelIdFromConfig = telegramConfig.getChannelId();
        if (channelIdFromConfig == null || channelIdFromConfig.isBlank()) {
            channelIdFromConfig = configLoader.getTelegramChannelId();
        }
        
        final String channelId = channelIdFromConfig;
        if (channelId == null || channelId.isBlank()) {
            log.warn("No Telegram channel configured");
            return;
        }
        
        CompletableFuture<Boolean> future = telegramBotClient.sendAlert(channelId, message);
        
        future.thenAccept(success -> {
            if (success) {
                log.info("Alert sent successfully for {} to {}", 
                        alert.prediction().symbol().getTicker(), channelId);
            } else {
                log.error("Failed to send alert for {}", alert.prediction().symbol().getTicker());
            }
        });
    }
    
    @Transactional
    public void markAlertSent(Long alertId) {
        alertRepository.findById(alertId).ifPresent(entity -> {
            entity.setStatus(AlertEntity.AlertStatus.SENT);
            alertRepository.save(entity);
        });
    }
    
    @Transactional
    public void markAlertFailed(Long alertId) {
        alertRepository.findById(alertId).ifPresent(entity -> {
            entity.setStatus(AlertEntity.AlertStatus.FAILED);
            alertRepository.save(entity);
        });
    }
    
    public boolean shouldSendAlert(Prediction prediction) {
        double threshold = configLoader.getAlertThreshold();
        
        return prediction.probability().doubleValue() >= threshold &&
               prediction.direction() != PredictionDirection.NEUTRAL;
    }
    
    private String formatVolume(Long volume) {
        if (volume >= 1_000_000_000) {
            return String.format("%.2fB", volume / 1_000_000_000.0);
        } else if (volume >= 1_000_000) {
            return String.format("%.2fM", volume / 1_000_000.0);
        } else if (volume >= 1_000) {
            return String.format("%.2fK", volume / 1_000.0);
        }
        return volume.toString();
    }
    
    private AlertStatus toAlertStatus(AlertEntity.AlertStatus status) {
        return switch (status) {
            case PENDING -> AlertStatus.PENDING;
            case SENT -> AlertStatus.SENT;
            case FAILED -> AlertStatus.FAILED;
            case DELIVERED -> AlertStatus.ACKNOWLEDGED;
            default -> AlertStatus.PENDING;
        };
    }
}

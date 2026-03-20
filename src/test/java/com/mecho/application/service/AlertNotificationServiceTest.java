package com.mecho.application.service;

import com.mecho.application.config.ConfigLoader;
import com.mecho.application.config.TelegramConfig;
import com.mecho.domain.alert.Alert;
import com.mecho.domain.model.AlertStatus;
import com.mecho.domain.model.PredictionDirection;
import com.mecho.domain.model.Symbol;
import com.mecho.domain.prediction.Prediction;
import com.mecho.infrastructure.persistence.entities.AlertEntity;
import com.mecho.infrastructure.persistence.repositories.AlertRepository;
import com.mecho.infrastructure.telegram.TelegramBotClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertNotificationServiceTest {

    @Mock
    private TelegramBotClient telegramBotClient;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private ConfigLoader configLoader;

    private TelegramConfig telegramConfig;
    private AlertNotificationService service;

    @BeforeEach
    void setUp() {
        telegramConfig = new TelegramConfig();
        telegramConfig.setEnabled(true);
        telegramConfig.setChannelId("-100123456789");

        service = new AlertNotificationService(
            telegramBotClient,
            alertRepository,
            telegramConfig,
            configLoader
        );
    }

    private Prediction createPrediction(PredictionDirection direction, double probability) {
        Symbol symbol = Symbol.builder().ticker("AAPL").build();
        return Prediction.create(symbol, direction, BigDecimal.valueOf(probability));
    }

    @Nested
    @DisplayName("Create and Send Alert Tests")
    class CreateAndSendAlertTests {

        @Test
        @DisplayName("Should create alert when probability above threshold")
        void shouldCreateAlertWhenProbabilityAboveThreshold() {
            Prediction prediction = createPrediction(PredictionDirection.UP, 0.8);
            when(configLoader.getAlertThreshold()).thenReturn(0.7);
            when(alertRepository.save(any())).thenAnswer(inv -> {
                AlertEntity entity = inv.getArgument(0);
                entity.setId(1L);
                return entity;
            });
            when(telegramBotClient.sendAlert(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(true));

            Alert result = service.createAndSendAlert(prediction, BigDecimal.valueOf(150), 1000000L);

            assertThat(result).isNotNull();
            verify(alertRepository).save(any());
        }

        @Test
        @DisplayName("Should return null when probability below threshold")
        void shouldReturnNullWhenProbabilityBelowThreshold() {
            Prediction prediction = createPrediction(PredictionDirection.UP, 0.5);
            when(configLoader.getAlertThreshold()).thenReturn(0.7);

            Alert result = service.createAndSendAlert(prediction, BigDecimal.valueOf(150), 1000000L);

            assertThat(result).isNull();
            verify(alertRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should return null for neutral prediction")
        void shouldReturnNullForNeutralPrediction() {
            Prediction prediction = createPrediction(PredictionDirection.NEUTRAL, 0.8);
            when(configLoader.getAlertThreshold()).thenReturn(0.7);

            Alert result = service.createAndSendAlert(prediction, BigDecimal.valueOf(150), 1000000L);

            assertThat(result).isNull();
            verify(alertRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Format Alert Message Tests")
    class FormatAlertMessageTests {

        @Test
        @DisplayName("Should format alert message with all fields")
        void shouldFormatAlertMessageWithAllFields() {
            Prediction prediction = createPrediction(PredictionDirection.UP, 0.75);
            prediction = Prediction.create(prediction.symbol(), prediction.direction(), 
                prediction.probability(), BigDecimal.valueOf(0.85));

            String message = service.formatAlertMessage(prediction, BigDecimal.valueOf(150.50), 1500000L);

            assertThat(message).contains("TRADING ALERT");
            assertThat(message).contains("AAPL");
            assertThat(message).contains("BULLISH");
            assertThat(message).contains("75%");
            assertThat(message).contains("85%");
            assertThat(message).contains("150.50");
            assertThat(message).contains("1.50M");
        }

        @Test
        @DisplayName("Should format alert message without confidence")
        void shouldFormatAlertMessageWithoutConfidence() {
            Prediction prediction = createPrediction(PredictionDirection.DOWN, 0.65);

            String message = service.formatAlertMessage(prediction, BigDecimal.valueOf(145.00), 500000L);

            assertThat(message).contains("BEARISH");
            assertThat(message).contains("65%");
            assertThat(message).doesNotContain("Confidence");
        }

        @Test
        @DisplayName("Should format alert message without price")
        void shouldFormatAlertMessageWithoutPrice() {
            Prediction prediction = createPrediction(PredictionDirection.UP, 0.80);

            String message = service.formatAlertMessage(prediction, null, 1000000L);

            assertThat(message).doesNotContain("Price:");
        }

        @Test
        @DisplayName("Should format alert message without volume")
        void shouldFormatAlertMessageWithoutVolume() {
            Prediction prediction = createPrediction(PredictionDirection.UP, 0.80);

            String message = service.formatAlertMessage(prediction, BigDecimal.valueOf(150), null);

            assertThat(message).doesNotContain("Volume:");
        }
    }

    @Nested
    @DisplayName("Format Simple Alert Message Tests")
    class FormatSimpleAlertMessageTests {

        @Test
        @DisplayName("Should format simple bullish message")
        void shouldFormatSimpleBullishMessage() {
            Prediction prediction = createPrediction(PredictionDirection.UP, 0.85);

            String message = service.formatSimpleAlertMessage(prediction);

            assertThat(message).contains("AAPL");
            assertThat(message).contains("UP");
            assertThat(message).contains("85%");
        }

        @Test
        @DisplayName("Should format simple bearish message")
        void shouldFormatSimpleBearishMessage() {
            Prediction prediction = createPrediction(PredictionDirection.DOWN, 0.72);

            String message = service.formatSimpleAlertMessage(prediction);

            assertThat(message).contains("DOWN");
        }
    }

    @Nested
    @DisplayName("Should Send Alert Tests")
    class ShouldSendAlertTests {

        @Test
        @DisplayName("Should return true for valid prediction")
        void shouldReturnTrueForValidPrediction() {
            Prediction prediction = createPrediction(PredictionDirection.UP, 0.8);
            when(configLoader.getAlertThreshold()).thenReturn(0.7);

            boolean result = service.shouldSendAlert(prediction);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for low probability")
        void shouldReturnFalseForLowProbability() {
            Prediction prediction = createPrediction(PredictionDirection.UP, 0.5);
            when(configLoader.getAlertThreshold()).thenReturn(0.7);

            boolean result = service.shouldSendAlert(prediction);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for neutral direction")
        void shouldReturnFalseForNeutralDirection() {
            Prediction prediction = createPrediction(PredictionDirection.NEUTRAL, 0.9);
            when(configLoader.getAlertThreshold()).thenReturn(0.7);

            boolean result = service.shouldSendAlert(prediction);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Mark Alert Tests")
    class MarkAlertTests {

        @Test
        @DisplayName("Should mark alert as sent")
        void shouldMarkAlertAsSent() {
            AlertEntity entity = AlertEntity.builder()
                .status(AlertEntity.AlertStatus.PENDING)
                .build();
            when(alertRepository.findById(1L)).thenReturn(java.util.Optional.of(entity));
            when(alertRepository.save(any())).thenReturn(entity);

            service.markAlertSent(1L);

            ArgumentCaptor<AlertEntity> captor = ArgumentCaptor.forClass(AlertEntity.class);
            verify(alertRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(AlertEntity.AlertStatus.SENT);
        }

        @Test
        @DisplayName("Should mark alert as failed")
        void shouldMarkAlertAsFailed() {
            AlertEntity entity = AlertEntity.builder()
                .status(AlertEntity.AlertStatus.PENDING)
                .build();
            when(alertRepository.findById(1L)).thenReturn(java.util.Optional.of(entity));
            when(alertRepository.save(any())).thenReturn(entity);

            service.markAlertFailed(1L);

            ArgumentCaptor<AlertEntity> captor = ArgumentCaptor.forClass(AlertEntity.class);
            verify(alertRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(AlertEntity.AlertStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("Volume Formatting Tests")
    class VolumeFormattingTests {

        @Test
        @DisplayName("Should format billions")
        void shouldFormatBillions() {
            Prediction prediction = createPrediction(PredictionDirection.UP, 0.8);
            String message = service.formatAlertMessage(prediction, null, 2_500_000_000L);
            assertThat(message).contains("2.50B");
        }

        @Test
        @DisplayName("Should format millions")
        void shouldFormatMillions() {
            Prediction prediction = createPrediction(PredictionDirection.UP, 0.8);
            String message = service.formatAlertMessage(prediction, null, 2_500_000L);
            assertThat(message).contains("2.50M");
        }

        @Test
        @DisplayName("Should format thousands")
        void shouldFormatThousands() {
            Prediction prediction = createPrediction(PredictionDirection.UP, 0.8);
            String message = service.formatAlertMessage(prediction, null, 2_500L);
            assertThat(message).contains("2.50K");
        }

        @Test
        @DisplayName("Should format small volumes")
        void shouldFormatSmallVolumes() {
            Prediction prediction = createPrediction(PredictionDirection.UP, 0.8);
            String message = service.formatAlertMessage(prediction, null, 500L);
            assertThat(message).contains("500");
            assertThat(message).doesNotContain("K");
        }
    }
}

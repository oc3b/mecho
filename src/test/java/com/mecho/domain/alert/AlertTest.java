package com.mecho.domain.alert;

import com.mecho.domain.model.AlertStatus;
import com.mecho.domain.model.PredictionDirection;
import com.mecho.domain.model.Symbol;
import com.mecho.domain.prediction.Prediction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AlertTest {

    private Prediction createTestPrediction() {
        Symbol symbol = Symbol.builder()
                .id(1L)
                .ticker("AAPL")
                .build();
        return Prediction.create(symbol, PredictionDirection.UP, java.math.BigDecimal.valueOf(0.75));
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create Alert with all values")
        void shouldCreateAlertWithAllValues() {
            Prediction prediction = createTestPrediction();
            Instant sentAt = Instant.now();

            Alert alert = new Alert(1L, prediction, sentAt, "TELEGRAM", AlertStatus.SENT);

            assertThat(alert.id()).isEqualTo(1L);
            assertThat(alert.prediction()).isEqualTo(prediction);
            assertThat(alert.sentAt()).isEqualTo(sentAt);
            assertThat(alert.channel()).isEqualTo("TELEGRAM");
            assertThat(alert.status()).isEqualTo(AlertStatus.SENT);
        }

        @Test
        @DisplayName("Should set default sentAt when null")
        void shouldSetDefaultSentAtWhenNull() {
            Prediction prediction = createTestPrediction();
            Instant before = Instant.now();
            Alert alert = new Alert(1L, prediction, null, "TELEGRAM", AlertStatus.PENDING);
            Instant after = Instant.now();

            assertThat(alert.sentAt()).isAfterOrEqualTo(before);
            assertThat(alert.sentAt()).isBeforeOrEqualTo(after);
        }

        @Test
        @DisplayName("Should set default channel when null")
        void shouldSetDefaultChannelWhenNull() {
            Prediction prediction = createTestPrediction();
            Alert alert = new Alert(1L, prediction, Instant.now(), null, AlertStatus.PENDING);

            assertThat(alert.channel()).isEqualTo("TELEGRAM");
        }

        @Test
        @DisplayName("Should set default status when null")
        void shouldSetDefaultStatusWhenNull() {
            Prediction prediction = createTestPrediction();
            Alert alert = new Alert(1L, prediction, Instant.now(), "TELEGRAM", null);

            assertThat(alert.status()).isEqualTo(AlertStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create alert with prediction and channel")
        void shouldCreateAlertWithPredictionAndChannel() {
            Prediction prediction = createTestPrediction();

            Alert alert = Alert.create(prediction, "SLACK");

            assertThat(alert.prediction()).isEqualTo(prediction);
            assertThat(alert.channel()).isEqualTo("SLACK");
            assertThat(alert.status()).isEqualTo(AlertStatus.PENDING);
            assertThat(alert.sentAt()).isNotNull();
        }

        @Test
        @DisplayName("Should create alert with null channel")
        void shouldCreateAlertWithNullChannel() {
            Prediction prediction = createTestPrediction();

            Alert alert = Alert.create(prediction, null);

            assertThat(alert.channel()).isEqualTo("TELEGRAM");
        }
    }

    @Nested
    @DisplayName("Status Transition Tests")
    class StatusTransitionTests {

        @Test
        @DisplayName("Should mark alert as sent")
        void shouldMarkAlertAsSent() {
            Prediction prediction = createTestPrediction();
            Alert alert = Alert.create(prediction, "TELEGRAM");
            Instant originalSentAt = alert.sentAt();

            Alert markedAlert = alert.markSent();

            assertThat(markedAlert.status()).isEqualTo(AlertStatus.SENT);
            assertThat(markedAlert.sentAt()).isAfterOrEqualTo(originalSentAt);
            assertThat(markedAlert.prediction()).isEqualTo(prediction);
        }

        @Test
        @DisplayName("Should mark alert as failed")
        void shouldMarkAlertAsFailed() {
            Prediction prediction = createTestPrediction();
            Alert alert = Alert.create(prediction, "TELEGRAM");
            Instant originalSentAt = alert.sentAt();

            Alert markedAlert = alert.markFailed();

            assertThat(markedAlert.status()).isEqualTo(AlertStatus.FAILED);
            assertThat(markedAlert.sentAt()).isEqualTo(originalSentAt);
            assertThat(markedAlert.prediction()).isEqualTo(prediction);
        }

        @Test
        @DisplayName("Should preserve id when marking as sent")
        void shouldPreserveIdWhenMarkingAsSent() {
            Prediction prediction = createTestPrediction();
            Alert alert = new Alert(42L, prediction, Instant.now(), "TELEGRAM", AlertStatus.PENDING);

            Alert markedAlert = alert.markSent();

            assertThat(markedAlert.id()).isEqualTo(42L);
        }

        @Test
        @DisplayName("Should preserve id when marking as failed")
        void shouldPreserveIdWhenMarkingAsFailed() {
            Prediction prediction = createTestPrediction();
            Alert alert = new Alert(42L, prediction, Instant.now(), "TELEGRAM", AlertStatus.PENDING);

            Alert markedAlert = alert.markFailed();

            assertThat(markedAlert.id()).isEqualTo(42L);
        }
    }

    @Nested
    @DisplayName("Immutability Tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("Original alert should be unchanged after markSent")
        void originalAlertShouldBeUnchangedAfterMarkSent() {
            Prediction prediction = createTestPrediction();
            Alert originalAlert = Alert.create(prediction, "TELEGRAM");

            Alert markedAlert = originalAlert.markSent();

            assertThat(originalAlert.status()).isEqualTo(AlertStatus.PENDING);
            assertThat(markedAlert.status()).isEqualTo(AlertStatus.SENT);
        }

        @Test
        @DisplayName("Original alert should be unchanged after markFailed")
        void originalAlertShouldBeUnchangedAfterMarkFailed() {
            Prediction prediction = createTestPrediction();
            Alert originalAlert = Alert.create(prediction, "TELEGRAM");

            Alert markedAlert = originalAlert.markFailed();

            assertThat(originalAlert.status()).isEqualTo(AlertStatus.PENDING);
            assertThat(markedAlert.status()).isEqualTo(AlertStatus.FAILED);
        }
    }
}

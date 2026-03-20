package com.mecho.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnumsTest {

    @Nested
    @DisplayName("AssetClass Enum Tests")
    class AssetClassTests {

        @Test
        @DisplayName("Should have all expected values")
        void shouldHaveAllExpectedValues() {
            AssetClass[] values = AssetClass.values();
            assertThat(values).hasSize(8);
        }

        @Test
        @DisplayName("Should contain EQUITY")
        void shouldContainEquity() {
            assertThat(AssetClass.valueOf("EQUITY")).isEqualTo(AssetClass.EQUITY);
        }

        @Test
        @DisplayName("Should contain FOREX")
        void shouldContainForex() {
            assertThat(AssetClass.valueOf("FOREX")).isEqualTo(AssetClass.FOREX);
        }

        @Test
        @DisplayName("Should contain CRYPTO")
        void shouldContainCrypto() {
            assertThat(AssetClass.valueOf("CRYPTO")).isEqualTo(AssetClass.CRYPTO);
        }

        @Test
        @DisplayName("Should contain COMMODITY")
        void shouldContainCommodity() {
            assertThat(AssetClass.valueOf("COMMODITY")).isEqualTo(AssetClass.COMMODITY);
        }

        @Test
        @DisplayName("Should contain INDEX")
        void shouldContainIndex() {
            assertThat(AssetClass.valueOf("INDEX")).isEqualTo(AssetClass.INDEX);
        }

        @Test
        @DisplayName("Should contain BOND")
        void shouldContainBond() {
            assertThat(AssetClass.valueOf("BOND")).isEqualTo(AssetClass.BOND);
        }

        @Test
        @DisplayName("Should contain ETF")
        void shouldContainEtf() {
            assertThat(AssetClass.valueOf("ETF")).isEqualTo(AssetClass.ETF);
        }

        @Test
        @DisplayName("Should contain FUTURES")
        void shouldContainFutures() {
            assertThat(AssetClass.valueOf("FUTURES")).isEqualTo(AssetClass.FUTURES);
        }
    }

    @Nested
    @DisplayName("AlertStatus Enum Tests")
    class AlertStatusTests {

        @Test
        @DisplayName("Should have all expected values")
        void shouldHaveAllExpectedValues() {
            AlertStatus[] values = AlertStatus.values();
            assertThat(values).hasSize(4);
        }

        @Test
        @DisplayName("Should contain PENDING")
        void shouldContainPending() {
            assertThat(AlertStatus.valueOf("PENDING")).isEqualTo(AlertStatus.PENDING);
        }

        @Test
        @DisplayName("Should contain SENT")
        void shouldContainSent() {
            assertThat(AlertStatus.valueOf("SENT")).isEqualTo(AlertStatus.SENT);
        }

        @Test
        @DisplayName("Should contain FAILED")
        void shouldContainFailed() {
            assertThat(AlertStatus.valueOf("FAILED")).isEqualTo(AlertStatus.FAILED);
        }

        @Test
        @DisplayName("Should contain ACKNOWLEDGED")
        void shouldContainAcknowledged() {
            assertThat(AlertStatus.valueOf("ACKNOWLEDGED")).isEqualTo(AlertStatus.ACKNOWLEDGED);
        }
    }

    @Nested
    @DisplayName("PredictionDirection Enum Tests")
    class PredictionDirectionTests {

        @Test
        @DisplayName("Should have all expected values")
        void shouldHaveAllExpectedValues() {
            PredictionDirection[] values = PredictionDirection.values();
            assertThat(values).hasSize(3);
        }

        @Test
        @DisplayName("Should contain UP")
        void shouldContainUp() {
            assertThat(PredictionDirection.valueOf("UP")).isEqualTo(PredictionDirection.UP);
        }

        @Test
        @DisplayName("Should contain DOWN")
        void shouldContainDown() {
            assertThat(PredictionDirection.valueOf("DOWN")).isEqualTo(PredictionDirection.DOWN);
        }

        @Test
        @DisplayName("Should contain NEUTRAL")
        void shouldContainNeutral() {
            assertThat(PredictionDirection.valueOf("NEUTRAL")).isEqualTo(PredictionDirection.NEUTRAL);
        }
    }
}

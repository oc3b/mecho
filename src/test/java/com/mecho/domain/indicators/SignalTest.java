package com.mecho.domain.indicators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SignalTest {

    @Test
    @DisplayName("Should have all expected enum values")
    void shouldHaveAllExpectedValues() {
        Signal[] signals = Signal.values();
        assertThat(signals).hasSize(3);
    }

    @Test
    @DisplayName("Should have BUY value")
    void shouldHaveBuyValue() {
        assertThat(Signal.BUY).isNotNull();
        assertThat(Signal.BUY.name()).isEqualTo("BUY");
    }

    @Test
    @DisplayName("Should have SELL value")
    void shouldHaveSellValue() {
        assertThat(Signal.SELL).isNotNull();
        assertThat(Signal.SELL.name()).isEqualTo("SELL");
    }

    @Test
    @DisplayName("Should have NEUTRAL value")
    void shouldHaveNeutralValue() {
        assertThat(Signal.NEUTRAL).isNotNull();
        assertThat(Signal.NEUTRAL.name()).isEqualTo("NEUTRAL");
    }

    @Test
    @DisplayName("Should find enum by name")
    void shouldFindByName() {
        assertThat(Signal.valueOf("BUY")).isEqualTo(Signal.BUY);
        assertThat(Signal.valueOf("SELL")).isEqualTo(Signal.SELL);
        assertThat(Signal.valueOf("NEUTRAL")).isEqualTo(Signal.NEUTRAL);
    }
}

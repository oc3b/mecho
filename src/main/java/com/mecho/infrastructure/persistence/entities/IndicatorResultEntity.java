package com.mecho.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "indicator_result", indexes = {
    @Index(name = "idx_indicator_symbol_time", columnList = "symbol_id, indicator_code, timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndicatorResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "symbol_id", nullable = false)
    private SymbolEntity symbol;

    @Column(name = "indicator_code", nullable = false, length = 50)
    private String indicatorCode;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(precision = 18, scale = 8)
    private BigDecimal value;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Signal signal;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public enum Signal {
        STRONG_BUY,
        BUY,
        NEUTRAL,
        SELL,
        STRONG_SELL
    }
}

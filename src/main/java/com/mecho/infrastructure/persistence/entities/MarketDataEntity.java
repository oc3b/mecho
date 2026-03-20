package com.mecho.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "market_data", indexes = {
    @Index(name = "idx_market_data_symbol_timestamp", columnList = "symbol_id, timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "symbol_id", nullable = false)
    private SymbolEntity symbol;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(precision = 18, scale = 8)
    private BigDecimal openPrice;

    @Column(precision = 18, scale = 8)
    private BigDecimal highPrice;

    @Column(precision = 18, scale = 8)
    private BigDecimal lowPrice;

    @Column(precision = 18, scale = 8)
    private BigDecimal closePrice;

    @Column(precision = 20, scale = 8)
    private BigDecimal volume;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", nullable = false, length = 50)
    private DataSource dataSource;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public enum DataSource {
        ALPHA_VANTAGE,
        COIN_GECKO,
        YAHOO_FINANCE,
        COINBASE,
        BINANCE,
        POLYGON,
        IEX_CLOUD,
        BLOOMBERG,
        CUSTOM
    }
}

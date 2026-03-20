package com.mecho.infrastructure.persistence.entities;

import com.mecho.domain.model.AssetClass;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "symbol")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SymbolEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String ticker;

    @Column(length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_class", length = 20)
    private AssetClass assetClass;

    @Column(length = 50)
    private String exchange;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "symbol", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MarketDataEntity> marketData = new ArrayList<>();

    @OneToMany(mappedBy = "symbol", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<IndicatorResultEntity> indicatorResults = new ArrayList<>();

    @OneToMany(mappedBy = "symbol", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PredictionEntity> predictions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (name == null) name = ticker;
        if (assetClass == null) assetClass = AssetClass.EQUITY;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

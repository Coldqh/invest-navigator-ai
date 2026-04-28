package com.investnavigator.backend.marketdata.model;

import com.investnavigator.backend.asset.model.Asset;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "candles",
        indexes = {
                @Index(name = "idx_candles_asset_id", columnList = "asset_id"),
                @Index(name = "idx_candles_timeframe", columnList = "timeframe"),
                @Index(name = "idx_candles_timestamp", columnList = "timestamp")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_candles_asset_timeframe_timestamp",
                        columnNames = {"asset_id", "timeframe", "timestamp"}
                )
        }
)
public class Candle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Timeframe timeframe;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal open;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal high;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal low;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal close;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal volume;

    @Column(nullable = false, length = 64)
    private String source;

    @Column(nullable = false)
    private Instant timestamp;
}
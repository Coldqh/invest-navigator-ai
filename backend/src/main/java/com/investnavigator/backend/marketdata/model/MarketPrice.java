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
        name = "market_prices",
        indexes = {
                @Index(name = "idx_market_prices_asset_id", columnList = "asset_id"),
                @Index(name = "idx_market_prices_timestamp", columnList = "timestamp")
        }
)
public class MarketPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal price;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal volume;

    @Column(nullable = false, length = 64)
    private String source;

    @Column(nullable = false)
    private Instant timestamp;
}
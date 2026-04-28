package com.investnavigator.backend.ai.model;

import com.investnavigator.backend.analytics.model.RiskLevel;
import com.investnavigator.backend.asset.model.Asset;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "ai_reports",
        indexes = {
                @Index(name = "idx_ai_reports_asset_id", columnList = "asset_id"),
                @Index(name = "idx_ai_reports_created_at", columnList = "created_at")
        }
)
public class AIReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Builder.Default
    @ElementCollection
    @CollectionTable(
            name = "ai_report_positive_factors",
            joinColumns = @JoinColumn(name = "report_id")
    )
    @OrderColumn(name = "factor_order")
    @Column(name = "factor_text", nullable = false, length = 1000)
    private List<String> positiveFactors = new ArrayList<>();

    @Builder.Default
    @ElementCollection
    @CollectionTable(
            name = "ai_report_negative_factors",
            joinColumns = @JoinColumn(name = "report_id")
    )
    @OrderColumn(name = "factor_order")
    @Column(name = "factor_text", nullable = false, length = 1000)
    private List<String> negativeFactors = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RiskLevel riskLevel;

    @Column(nullable = false)
    private int riskScore;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal confidence;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String disclaimer;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
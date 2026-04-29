package com.investnavigator.backend.ai.model;

import com.investnavigator.backend.ai.provider.AIProviderType;
import com.investnavigator.backend.analytics.model.RiskLevel;
import com.investnavigator.backend.asset.model.Asset;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ai_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_provider", nullable = false, length = 50)
    @Builder.Default
    private AIProviderType aiProvider = AIProviderType.MOCK;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "ai_report_positive_factors",
            joinColumns = @JoinColumn(name = "report_id")
    )
    @Column(name = "positive_factor", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private List<String> positiveFactors = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "ai_report_negative_factors",
            joinColumns = @JoinColumn(name = "report_id")
    )
    @Column(name = "negative_factor", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private List<String> negativeFactors = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 30)
    private RiskLevel riskLevel;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal confidence;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String disclaimer;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
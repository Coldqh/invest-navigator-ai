package com.investnavigator.backend.ai.dto;

import com.investnavigator.backend.ai.provider.AIProviderType;
import com.investnavigator.backend.analytics.model.RiskLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AIReportResponse(
        UUID id,
        UUID assetId,
        String ticker,
        String name,
        AIProviderType provider,
        String summary,
        List<String> positiveFactors,
        List<String> negativeFactors,
        RiskLevel riskLevel,
        Integer riskScore,
        BigDecimal confidence,
        String explanation,
        String disclaimer,
        Instant createdAt
) {
}
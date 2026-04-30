package com.investnavigator.backend.ai.dto;

import com.investnavigator.backend.ai.provider.AIProviderType;
import com.investnavigator.backend.ai.provider.dto.AICompareAssetSnapshot;
import com.investnavigator.backend.analytics.model.RiskLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AICompareReportResponse(
        AIProviderType provider,
        int assetsCount,
        List<AICompareAssetSnapshot> assets,
        String summary,
        List<String> positiveFactors,
        List<String> negativeFactors,
        RiskLevel riskLevel,
        Integer riskScore,
        BigDecimal confidence,
        String explanation,
        String disclaimer,
        String fallbackReason,
        Instant generatedAt
) {
}
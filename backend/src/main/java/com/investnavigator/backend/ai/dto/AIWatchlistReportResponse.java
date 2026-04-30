package com.investnavigator.backend.ai.dto;

import com.investnavigator.backend.ai.provider.AIProviderType;
import com.investnavigator.backend.ai.provider.dto.AIWatchlistItemSnapshot;
import com.investnavigator.backend.analytics.model.RiskLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AIWatchlistReportResponse(
        AIProviderType provider,
        int itemsCount,
        List<AIWatchlistItemSnapshot> items,
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
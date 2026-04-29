package com.investnavigator.backend.ai.dto;

import com.investnavigator.backend.ai.provider.AIProviderType;
import com.investnavigator.backend.analytics.model.RiskLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AIPortfolioReportResponse(
        AIProviderType provider,
        int positionsCount,
        BigDecimal totalInvested,
        BigDecimal totalCurrentValue,
        BigDecimal totalProfitLoss,
        BigDecimal totalProfitLossPercent,
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
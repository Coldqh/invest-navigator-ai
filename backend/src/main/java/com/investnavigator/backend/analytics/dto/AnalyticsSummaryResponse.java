package com.investnavigator.backend.analytics.dto;

import com.investnavigator.backend.analytics.model.RiskLevel;

import java.math.BigDecimal;

public record AnalyticsSummaryResponse(
        String ticker,
        String name,
        BigDecimal currentPrice,
        BigDecimal firstClose,
        BigDecimal lastClose,
        BigDecimal priceChange,
        BigDecimal priceChangePercent,
        BigDecimal averageVolume,
        BigDecimal volatilityPercent,
        int riskScore,
        RiskLevel riskLevel,
        int dataPoints
) {
}
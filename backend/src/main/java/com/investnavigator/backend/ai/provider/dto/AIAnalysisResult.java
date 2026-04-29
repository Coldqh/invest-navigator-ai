package com.investnavigator.backend.ai.provider.dto;

import com.investnavigator.backend.analytics.model.RiskLevel;

import java.math.BigDecimal;
import java.util.List;

public record AIAnalysisResult(
        String summary,
        List<String> positiveFactors,
        List<String> negativeFactors,
        RiskLevel riskLevel,
        int riskScore,
        BigDecimal confidence,
        String explanation,
        String disclaimer
) {
}
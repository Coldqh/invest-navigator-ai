package com.investnavigator.backend.ai.provider.dto;

import com.investnavigator.backend.analytics.model.RiskLevel;

import java.math.BigDecimal;
import java.util.List;

public record AIProviderJsonPayload(
        String summary,
        List<String> positiveFactors,
        List<String> negativeFactors,
        RiskLevel riskLevel,
        Integer riskScore,
        BigDecimal confidence,
        String explanation,
        String disclaimer
) {
}
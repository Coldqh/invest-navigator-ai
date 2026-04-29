package com.investnavigator.backend.ai.provider.dto;

import com.investnavigator.backend.analytics.dto.AnalyticsSummaryResponse;

public record AIAnalysisRequest(
        String ticker,
        String name,
        AnalyticsSummaryResponse analytics
) {
}
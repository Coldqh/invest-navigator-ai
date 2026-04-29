package com.investnavigator.backend.ai.provider.dto;

import com.investnavigator.backend.portfolio.dto.PortfolioSummaryResponse;

public record AIPortfolioAnalysisRequest(
        PortfolioSummaryResponse portfolio
) {
}
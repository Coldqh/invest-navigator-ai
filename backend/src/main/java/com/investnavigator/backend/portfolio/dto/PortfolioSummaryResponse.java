package com.investnavigator.backend.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PortfolioSummaryResponse(
        int positionsCount,
        BigDecimal totalInvested,
        BigDecimal totalCurrentValue,
        BigDecimal totalProfitLoss,
        BigDecimal totalProfitLossPercent,
        List<PortfolioPositionResponse> positions,
        Instant calculatedAt
) {
}
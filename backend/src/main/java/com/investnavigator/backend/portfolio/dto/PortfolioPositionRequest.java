package com.investnavigator.backend.portfolio.dto;

import java.math.BigDecimal;

public record PortfolioPositionRequest(
        String ticker,
        BigDecimal quantity,
        BigDecimal averageBuyPrice
) {
}
package com.investnavigator.backend.portfolio.dto;

import com.investnavigator.backend.asset.model.AssetType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PortfolioPositionResponse(
        UUID id,
        UUID assetId,
        String ticker,
        String name,
        AssetType assetType,
        String exchange,
        String currency,
        BigDecimal quantity,
        BigDecimal averageBuyPrice,
        BigDecimal investedAmount,
        BigDecimal currentPrice,
        BigDecimal currentValue,
        BigDecimal profitLoss,
        BigDecimal profitLossPercent,
        String priceSource,
        Instant priceTimestamp,
        Instant createdAt,
        Instant updatedAt
) {
}
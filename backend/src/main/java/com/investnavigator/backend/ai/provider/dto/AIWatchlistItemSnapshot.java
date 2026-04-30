package com.investnavigator.backend.ai.provider.dto;

import com.investnavigator.backend.asset.model.AssetType;

import java.math.BigDecimal;
import java.time.Instant;

public record AIWatchlistItemSnapshot(
        String ticker,
        String name,
        AssetType assetType,
        String exchange,
        String currency,
        BigDecimal latestPrice,
        BigDecimal latestVolume,
        String priceSource,
        Instant priceTimestamp,
        String dataError
) {
}
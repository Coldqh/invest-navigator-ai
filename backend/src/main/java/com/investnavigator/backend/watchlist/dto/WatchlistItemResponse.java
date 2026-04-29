package com.investnavigator.backend.watchlist.dto;

import com.investnavigator.backend.asset.model.AssetType;

import java.time.Instant;
import java.util.UUID;

public record WatchlistItemResponse(
        UUID id,
        UUID assetId,
        String ticker,
        String name,
        AssetType assetType,
        String exchange,
        String currency,
        String isin,
        boolean active,
        Instant createdAt
) {
}
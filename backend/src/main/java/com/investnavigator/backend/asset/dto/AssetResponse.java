package com.investnavigator.backend.asset.dto;

import com.investnavigator.backend.asset.model.AssetType;

import java.util.UUID;

public record AssetResponse(
        UUID id,
        String ticker,
        String name,
        AssetType assetType,
        String exchange,
        String currency,
        String isin,
        boolean active
) {
}
package com.investnavigator.backend.marketdata.dto;

import com.investnavigator.backend.marketdata.provider.MarketDataProviderType;

import java.time.Instant;

public record MarketDataProviderHealthItemResponse(
        MarketDataProviderType type,
        ProviderHealthStatus status,
        String message,
        Instant checkedAt
) {
}
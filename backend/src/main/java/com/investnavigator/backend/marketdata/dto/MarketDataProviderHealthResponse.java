package com.investnavigator.backend.marketdata.dto;

import com.investnavigator.backend.marketdata.provider.MarketDataProviderType;

import java.time.Instant;
import java.util.List;

public record MarketDataProviderHealthResponse(
        MarketDataProviderType activeProvider,
        ProviderHealthStatus status,
        List<MarketDataProviderHealthItemResponse> providers,
        Instant checkedAt
) {
}
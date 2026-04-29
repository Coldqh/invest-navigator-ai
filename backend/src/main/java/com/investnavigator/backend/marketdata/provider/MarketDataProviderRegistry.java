package com.investnavigator.backend.marketdata.provider;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketDataProviderRegistry {

    private final List<MarketDataProvider> providers;

    public MarketDataProvider getProvider(MarketDataProviderType type) {
        return providers.stream()
                .filter(provider -> provider.getType() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Market data provider not found: " + type
                ));
    }
}
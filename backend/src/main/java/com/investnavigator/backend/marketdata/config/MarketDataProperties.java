package com.investnavigator.backend.marketdata.config;

import com.investnavigator.backend.marketdata.provider.MarketDataProviderType;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "market-data")
public record MarketDataProperties(
        MarketDataProviderType provider
) {
    public MarketDataProperties {
        if (provider == null) {
            provider = MarketDataProviderType.DEMO;
        }
    }
}
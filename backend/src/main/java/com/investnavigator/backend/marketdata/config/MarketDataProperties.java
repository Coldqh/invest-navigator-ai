package com.investnavigator.backend.marketdata.config;

import com.investnavigator.backend.marketdata.provider.MarketDataProviderType;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "market-data")
public record MarketDataProperties(
        MarketDataProviderType provider,
        Binance binance,
        Moex moex
) {
    public MarketDataProperties {
        if (provider == null) {
            provider = MarketDataProviderType.DEMO;
        }

        if (binance == null) {
            binance = new Binance("https://api.binance.com");
        }

        if (moex == null) {
            moex = new Moex("https://iss.moex.com");
        }
    }

    public record Binance(
            String baseUrl
    ) {
        public Binance {
            if (baseUrl == null || baseUrl.isBlank()) {
                baseUrl = "https://api.binance.com";
            }
        }
    }

    public record Moex(
            String baseUrl
    ) {
        public Moex {
            if (baseUrl == null || baseUrl.isBlank()) {
                baseUrl = "https://iss.moex.com";
            }
        }
    }
}
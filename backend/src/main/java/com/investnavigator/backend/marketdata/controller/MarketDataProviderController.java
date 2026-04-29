package com.investnavigator.backend.marketdata.controller;

import com.investnavigator.backend.marketdata.config.MarketDataProperties;
import com.investnavigator.backend.marketdata.provider.MarketDataProvider;
import com.investnavigator.backend.marketdata.provider.MarketDataProviderRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/market-data/provider")
@RequiredArgsConstructor
public class MarketDataProviderController {

    private final MarketDataProperties marketDataProperties;
    private final MarketDataProviderRegistry marketDataProviderRegistry;

    @GetMapping
    public Map<String, Object> getActiveProvider() {
        MarketDataProvider provider = marketDataProviderRegistry.getProvider(
                marketDataProperties.provider()
        );

        return Map.of(
                "activeProvider", provider.getType(),
                "status", "AVAILABLE"
        );
    }
}
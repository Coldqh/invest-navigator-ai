package com.investnavigator.backend.marketdata.controller;

import com.investnavigator.backend.marketdata.config.MarketDataProperties;
import com.investnavigator.backend.marketdata.dto.MarketDataProviderHealthResponse;
import com.investnavigator.backend.marketdata.provider.MarketDataProvider;
import com.investnavigator.backend.marketdata.provider.MarketDataProviderRegistry;
import com.investnavigator.backend.marketdata.service.MarketDataProviderHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/market-data/provider")
@RequiredArgsConstructor
public class MarketDataProviderController {

    private final MarketDataProperties marketDataProperties;
    private final MarketDataProviderRegistry marketDataProviderRegistry;
    private final MarketDataProviderHealthService marketDataProviderHealthService;

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

    @GetMapping("/health")
    public MarketDataProviderHealthResponse getProviderHealth() {
        return marketDataProviderHealthService.getHealth();
    }
}
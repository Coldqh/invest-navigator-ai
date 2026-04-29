package com.investnavigator.backend.marketdata.controller;

import com.investnavigator.backend.marketdata.dto.MarketPriceResponse;
import com.investnavigator.backend.marketdata.service.MarketDataRefreshService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market-data")
@RequiredArgsConstructor
public class MarketDataRefreshController {

    private final MarketDataRefreshService marketDataRefreshService;

    @PostMapping("/refresh/{ticker}")
    public MarketPriceResponse refreshLatestPrice(@PathVariable String ticker) {
        return marketDataRefreshService.refreshLatestPrice(ticker);
    }
}
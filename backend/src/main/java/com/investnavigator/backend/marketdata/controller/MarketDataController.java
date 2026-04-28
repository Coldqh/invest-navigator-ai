package com.investnavigator.backend.marketdata.controller;

import com.investnavigator.backend.marketdata.dto.CandleResponse;
import com.investnavigator.backend.marketdata.dto.MarketPriceResponse;
import com.investnavigator.backend.marketdata.model.Timeframe;
import com.investnavigator.backend.marketdata.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assets/{ticker}")
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataService marketDataService;

    @GetMapping("/market-data")
    public MarketPriceResponse getLatestMarketPrice(@PathVariable String ticker) {
        return marketDataService.getLatestMarketPrice(ticker);
    }

    @GetMapping("/candles")
    public List<CandleResponse> getCandles(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "1D") String timeframe
    ) {
        return marketDataService.getCandles(ticker, parseTimeframe(timeframe));
    }

    private Timeframe parseTimeframe(String timeframe) {
        return switch (timeframe.toUpperCase()) {
            case "1M" -> Timeframe.ONE_MINUTE;
            case "5M" -> Timeframe.FIVE_MINUTES;
            case "15M" -> Timeframe.FIFTEEN_MINUTES;
            case "1H" -> Timeframe.ONE_HOUR;
            case "1D" -> Timeframe.ONE_DAY;
            default -> throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
        };
    }
}
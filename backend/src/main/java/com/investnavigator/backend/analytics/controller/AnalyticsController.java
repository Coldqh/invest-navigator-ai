package com.investnavigator.backend.analytics.controller;

import com.investnavigator.backend.analytics.dto.AnalyticsSummaryResponse;
import com.investnavigator.backend.analytics.dto.CompareAssetsRequest;
import com.investnavigator.backend.analytics.service.AnalyticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/{ticker}/summary")
    public AnalyticsSummaryResponse getSummary(@PathVariable String ticker) {
        return analyticsService.getSummary(ticker);
    }

    @PostMapping("/compare")
    public List<AnalyticsSummaryResponse> compareAssets(
            @Valid @RequestBody CompareAssetsRequest request
    ) {
        return analyticsService.compareAssets(request.tickers());
    }
}
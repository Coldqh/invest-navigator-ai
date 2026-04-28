package com.investnavigator.backend.analytics.controller;

import com.investnavigator.backend.analytics.dto.AnalyticsSummaryResponse;
import com.investnavigator.backend.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/{ticker}/summary")
    public AnalyticsSummaryResponse getSummary(@PathVariable String ticker) {
        return analyticsService.getSummary(ticker);
    }
}
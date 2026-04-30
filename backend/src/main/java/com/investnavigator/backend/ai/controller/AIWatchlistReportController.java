package com.investnavigator.backend.ai.controller;

import com.investnavigator.backend.ai.dto.AIWatchlistReportResponse;
import com.investnavigator.backend.ai.service.AIWatchlistReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/watchlist")
@RequiredArgsConstructor
public class AIWatchlistReportController {

    private final AIWatchlistReportService aiWatchlistReportService;

    @PostMapping("/analyze")
    public AIWatchlistReportResponse generateWatchlistReport() {
        return aiWatchlistReportService.generateWatchlistReport();
    }
}
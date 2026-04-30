package com.investnavigator.backend.ai.controller;

import com.investnavigator.backend.ai.dto.AICompareReportResponse;
import com.investnavigator.backend.ai.service.AICompareReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/compare")
@RequiredArgsConstructor
public class AICompareReportController {

    private final AICompareReportService aiCompareReportService;

    @PostMapping("/analyze")
    public AICompareReportResponse generateCompareReport(
            @RequestParam String firstTicker,
            @RequestParam String secondTicker
    ) {
        return aiCompareReportService.generateCompareReport(firstTicker, secondTicker);
    }
}
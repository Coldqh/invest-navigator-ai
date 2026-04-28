package com.investnavigator.backend.ai.controller;

import com.investnavigator.backend.ai.dto.AIReportResponse;
import com.investnavigator.backend.ai.service.AIReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIReportController {

    private final AIReportService aiReportService;

    @PostMapping("/analyze/{ticker}")
    public AIReportResponse generateReport(@PathVariable String ticker) {
        return aiReportService.generateReport(ticker);
    }

    @GetMapping("/reports/{ticker}")
    public List<AIReportResponse> getReportsByTicker(@PathVariable String ticker) {
        return aiReportService.getReportsByTicker(ticker);
    }

    @GetMapping("/reports/details/{reportId}")
    public AIReportResponse getReportById(@PathVariable UUID reportId) {
        return aiReportService.getReportById(reportId);
    }
}
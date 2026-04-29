package com.investnavigator.backend.ai.controller;

import com.investnavigator.backend.ai.dto.AIPortfolioReportResponse;
import com.investnavigator.backend.ai.service.AIPortfolioReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/portfolio")
@RequiredArgsConstructor
public class AIPortfolioReportController {

    private final AIPortfolioReportService aiPortfolioReportService;

    @PostMapping("/analyze")
    public AIPortfolioReportResponse generatePortfolioReport() {
        return aiPortfolioReportService.generatePortfolioReport();
    }
}
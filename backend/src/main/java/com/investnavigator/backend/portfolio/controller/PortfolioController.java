package com.investnavigator.backend.portfolio.controller;

import com.investnavigator.backend.portfolio.dto.PortfolioPositionRequest;
import com.investnavigator.backend.portfolio.dto.PortfolioPositionResponse;
import com.investnavigator.backend.portfolio.dto.PortfolioSummaryResponse;
import com.investnavigator.backend.portfolio.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping
    public PortfolioSummaryResponse getPortfolio() {
        return portfolioService.getPortfolio();
    }

    @PostMapping("/positions")
    public PortfolioPositionResponse savePosition(
            @RequestBody PortfolioPositionRequest request
    ) {
        return portfolioService.savePosition(request);
    }

    @DeleteMapping("/positions/{ticker}")
    public ResponseEntity<Void> deletePosition(@PathVariable String ticker) {
        portfolioService.deletePosition(ticker);

        return ResponseEntity.noContent().build();
    }
}
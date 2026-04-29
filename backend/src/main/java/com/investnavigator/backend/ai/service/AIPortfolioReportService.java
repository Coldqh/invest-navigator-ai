package com.investnavigator.backend.ai.service;

import com.investnavigator.backend.ai.config.AIProperties;
import com.investnavigator.backend.ai.dto.AIPortfolioReportResponse;
import com.investnavigator.backend.ai.provider.AIProvider;
import com.investnavigator.backend.ai.provider.AIProviderRegistry;
import com.investnavigator.backend.ai.provider.AIProviderType;
import com.investnavigator.backend.ai.provider.dto.AIAnalysisResult;
import com.investnavigator.backend.ai.provider.dto.AIPortfolioAnalysisRequest;
import com.investnavigator.backend.common.error.BadRequestException;
import com.investnavigator.backend.portfolio.dto.PortfolioSummaryResponse;
import com.investnavigator.backend.portfolio.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AIPortfolioReportService {

    private static final Logger log = LoggerFactory.getLogger(AIPortfolioReportService.class);

    private final PortfolioService portfolioService;
    private final AIProperties aiProperties;
    private final AIProviderRegistry aiProviderRegistry;

    public AIPortfolioReportResponse generatePortfolioReport() {
        PortfolioSummaryResponse portfolio = portfolioService.getPortfolio();

        if (portfolio.positionsCount() == 0) {
            throw new BadRequestException("Portfolio is empty. Add at least one position before generating AI portfolio report.");
        }

        AnalysisWithProvider analysisWithProvider = analyzeWithFallback(portfolio);
        AIAnalysisResult analysisResult = analysisWithProvider.analysisResult();

        return new AIPortfolioReportResponse(
                analysisWithProvider.providerType(),
                portfolio.positionsCount(),
                portfolio.totalInvested(),
                portfolio.totalCurrentValue(),
                portfolio.totalProfitLoss(),
                portfolio.totalProfitLossPercent(),
                analysisResult.summary(),
                analysisResult.positiveFactors(),
                analysisResult.negativeFactors(),
                analysisResult.riskLevel(),
                analysisResult.riskScore(),
                analysisResult.confidence(),
                analysisResult.explanation(),
                analysisResult.disclaimer(),
                analysisWithProvider.fallbackReason(),
                Instant.now()
        );
    }

    private AnalysisWithProvider analyzeWithFallback(PortfolioSummaryResponse portfolio) {
        AIProviderType activeProviderType = aiProperties.provider();

        AIPortfolioAnalysisRequest request = new AIPortfolioAnalysisRequest(portfolio);

        AIProvider activeProvider = aiProviderRegistry.getProvider(activeProviderType);

        try {
            AIAnalysisResult analysisResult = activeProvider.analyzePortfolio(request);

            return new AnalysisWithProvider(
                    activeProviderType,
                    analysisResult,
                    null
            );
        } catch (RuntimeException exception) {
            if (activeProviderType == AIProviderType.MOCK) {
                throw exception;
            }

            String fallbackReason = buildFallbackReason(activeProviderType, exception);

            log.warn(
                    "AI provider {} failed for portfolio report. Falling back to MOCK provider. Reason: {}",
                    activeProviderType,
                    fallbackReason
            );

            AIProvider mockProvider = aiProviderRegistry.getProvider(AIProviderType.MOCK);

            return new AnalysisWithProvider(
                    AIProviderType.MOCK,
                    mockProvider.analyzePortfolio(request),
                    fallbackReason
            );
        }
    }

    private String buildFallbackReason(
            AIProviderType providerType,
            RuntimeException exception
    ) {
        if (exception instanceof RestClientResponseException restException) {
            return """
                    Provider %s HTTP request failed.
                    Status: %s
                    Response body: %s
                    """.formatted(
                    providerType,
                    restException.getStatusCode(),
                    restException.getResponseBodyAsString()
            ).trim();
        }

        return """
                Provider %s failed.
                Exception: %s
                Message: %s
                """.formatted(
                providerType,
                exception.getClass().getSimpleName(),
                exception.getMessage()
        ).trim();
    }

    private record AnalysisWithProvider(
            AIProviderType providerType,
            AIAnalysisResult analysisResult,
            String fallbackReason
    ) {
    }
}
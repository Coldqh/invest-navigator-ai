package com.investnavigator.backend.ai.service;

import com.investnavigator.backend.ai.config.AIProperties;
import com.investnavigator.backend.ai.dto.AICompareReportResponse;
import com.investnavigator.backend.ai.provider.AIProvider;
import com.investnavigator.backend.ai.provider.AIProviderRegistry;
import com.investnavigator.backend.ai.provider.AIProviderType;
import com.investnavigator.backend.ai.provider.dto.AIAnalysisResult;
import com.investnavigator.backend.ai.provider.dto.AICompareAnalysisRequest;
import com.investnavigator.backend.ai.provider.dto.AICompareAssetSnapshot;
import com.investnavigator.backend.analytics.dto.AnalyticsSummaryResponse;
import com.investnavigator.backend.analytics.service.AnalyticsService;
import com.investnavigator.backend.common.error.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AICompareReportService {

    private static final Logger log = LoggerFactory.getLogger(AICompareReportService.class);

    private final AnalyticsService analyticsService;
    private final AIProperties aiProperties;
    private final AIProviderRegistry aiProviderRegistry;

    public AICompareReportResponse generateCompareReport(
            String firstTicker,
            String secondTicker
    ) {
        validateTickers(firstTicker, secondTicker);

        List<AnalyticsSummaryResponse> summaries = analyticsService.compareAssets(
                List.of(firstTicker, secondTicker)
        );

        AICompareAnalysisRequest request = new AICompareAnalysisRequest(
                summaries.stream()
                        .map(this::toSnapshot)
                        .toList(),
                Instant.now()
        );

        AnalysisWithProvider analysisWithProvider = analyzeWithFallback(request);
        AIAnalysisResult analysisResult = analysisWithProvider.analysisResult();

        return new AICompareReportResponse(
                analysisWithProvider.providerType(),
                request.assets().size(),
                request.assets(),
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

    private AICompareAssetSnapshot toSnapshot(AnalyticsSummaryResponse summary) {
        return new AICompareAssetSnapshot(
                summary.ticker(),
                summary.name(),
                summary.currentPrice(),
                summary.firstClose(),
                summary.lastClose(),
                summary.priceChange(),
                summary.priceChangePercent(),
                summary.averageVolume(),
                summary.volatilityPercent(),
                summary.riskScore(),
                summary.riskLevel(),
                summary.dataPoints()
        );
    }

    private AnalysisWithProvider analyzeWithFallback(AICompareAnalysisRequest request) {
        AIProviderType activeProviderType = aiProperties.provider();

        AIProvider activeProvider = aiProviderRegistry.getProvider(activeProviderType);

        try {
            AIAnalysisResult analysisResult = activeProvider.analyzeCompare(request);

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
                    "AI provider {} failed for compare report. Falling back to MOCK provider. Reason: {}",
                    activeProviderType,
                    fallbackReason
            );

            AIProvider mockProvider = aiProviderRegistry.getProvider(AIProviderType.MOCK);

            return new AnalysisWithProvider(
                    AIProviderType.MOCK,
                    mockProvider.analyzeCompare(request),
                    fallbackReason
            );
        }
    }

    private void validateTickers(
            String firstTicker,
            String secondTicker
    ) {
        if (firstTicker == null || firstTicker.isBlank()) {
            throw new BadRequestException("First ticker is required");
        }

        if (secondTicker == null || secondTicker.isBlank()) {
            throw new BadRequestException("Second ticker is required");
        }

        if (firstTicker.trim().equalsIgnoreCase(secondTicker.trim())) {
            throw new BadRequestException("You must compare two different assets");
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
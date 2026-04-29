package com.investnavigator.backend.ai.provider.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investnavigator.backend.ai.provider.dto.AIAnalysisResult;
import com.investnavigator.backend.ai.provider.dto.AIProviderJsonPayload;
import com.investnavigator.backend.analytics.dto.AnalyticsSummaryResponse;
import com.investnavigator.backend.analytics.model.RiskLevel;
import com.investnavigator.backend.portfolio.dto.PortfolioSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AIProviderJsonParser {

    private static final BigDecimal DEFAULT_CONFIDENCE = BigDecimal.valueOf(0.50);
    private static final String DEFAULT_DISCLAIMER = """
            This report is generated for educational purposes only.
            It is not financial advice and should not be used as the only basis for investment decisions.
            """;

    private final ObjectMapper objectMapper;

    public AIAnalysisResult parse(String rawText, AnalyticsSummaryResponse fallbackAnalytics) {
        AIProviderJsonPayload payload = parsePayload(rawText);

        return new AIAnalysisResult(
                safeString(payload.summary(), buildFallbackSummary(fallbackAnalytics)),
                safeList(payload.positiveFactors(), "No positive factors were provided by the AI provider."),
                safeList(payload.negativeFactors(), "No negative factors were provided by the AI provider."),
                payload.riskLevel() == null ? fallbackAnalytics.riskLevel() : payload.riskLevel(),
                payload.riskScore() == null ? fallbackAnalytics.riskScore() : clampRiskScore(payload.riskScore()),
                safeConfidence(payload.confidence()),
                safeString(payload.explanation(), buildFallbackExplanation(fallbackAnalytics)),
                safeString(payload.disclaimer(), DEFAULT_DISCLAIMER)
        );
    }

    public AIAnalysisResult parsePortfolio(
            String rawText,
            PortfolioSummaryResponse fallbackPortfolio
    ) {
        AIProviderJsonPayload payload = parsePayload(rawText);

        int fallbackRiskScore = calculatePortfolioFallbackRiskScore(fallbackPortfolio);
        RiskLevel fallbackRiskLevel = calculateRiskLevel(fallbackRiskScore);

        return new AIAnalysisResult(
                safeString(payload.summary(), buildPortfolioFallbackSummary(fallbackPortfolio)),
                safeList(payload.positiveFactors(), "No positive portfolio factors were provided by the AI provider."),
                safeList(payload.negativeFactors(), "No negative portfolio factors were provided by the AI provider."),
                payload.riskLevel() == null ? fallbackRiskLevel : payload.riskLevel(),
                payload.riskScore() == null ? fallbackRiskScore : clampRiskScore(payload.riskScore()),
                safeConfidence(payload.confidence()),
                safeString(payload.explanation(), buildPortfolioFallbackExplanation(fallbackPortfolio, fallbackRiskScore, fallbackRiskLevel)),
                safeString(payload.disclaimer(), DEFAULT_DISCLAIMER)
        );
    }

    private AIProviderJsonPayload parsePayload(String rawText) {
        String json = extractJsonObject(rawText);

        try {
            return objectMapper.readValue(
                    json,
                    AIProviderJsonPayload.class
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "Failed to parse AI provider JSON response: " + exception.getMessage(),
                    exception
            );
        }
    }

    private String extractJsonObject(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("AI provider response is empty");
        }

        int start = rawText.indexOf('{');
        int end = rawText.lastIndexOf('}');

        if (start < 0 || end < 0 || end <= start) {
            throw new IllegalArgumentException("AI provider response does not contain a JSON object");
        }

        return rawText.substring(start, end + 1);
    }

    private String safeString(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }

    private List<String> safeList(List<String> values, String fallbackValue) {
        if (values == null || values.isEmpty()) {
            return List.of(fallbackValue);
        }

        List<String> cleanedValues = values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();

        if (cleanedValues.isEmpty()) {
            return List.of(fallbackValue);
        }

        return cleanedValues;
    }

    private int clampRiskScore(int riskScore) {
        if (riskScore < 0) {
            return 0;
        }

        return Math.min(riskScore, 100);
    }

    private BigDecimal safeConfidence(BigDecimal confidence) {
        if (confidence == null) {
            return DEFAULT_CONFIDENCE.setScale(2, RoundingMode.HALF_UP);
        }

        if (confidence.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        if (confidence.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);
        }

        return confidence.setScale(2, RoundingMode.HALF_UP);
    }

    private String buildFallbackSummary(AnalyticsSummaryResponse analytics) {
        return analytics.ticker() + " was analyzed using available market metrics.";
    }

    private String buildFallbackExplanation(AnalyticsSummaryResponse analytics) {
        return """
                Fallback explanation generated because the AI provider response did not include a valid explanation.
                
                Risk score: %s / 100
                Risk level: %s
                Data points: %s
                """.formatted(
                analytics.riskScore(),
                analytics.riskLevel(),
                analytics.dataPoints()
        );
    }

    private String buildPortfolioFallbackSummary(PortfolioSummaryResponse portfolio) {
        return "Portfolio was analyzed using available position metrics.";
    }

    private String buildPortfolioFallbackExplanation(
            PortfolioSummaryResponse portfolio,
            int riskScore,
            RiskLevel riskLevel
    ) {
        return """
                Fallback portfolio explanation generated because the AI provider response did not include a valid explanation.
                
                Positions count: %s
                Total invested: %s
                Total current value: %s
                Total profit/loss: %s
                Total profit/loss percent: %s%%
                Risk score: %s / 100
                Risk level: %s
                """.formatted(
                portfolio.positionsCount(),
                portfolio.totalInvested(),
                portfolio.totalCurrentValue(),
                portfolio.totalProfitLoss(),
                portfolio.totalProfitLossPercent(),
                riskScore,
                riskLevel
        );
    }

    private int calculatePortfolioFallbackRiskScore(PortfolioSummaryResponse portfolio) {
        int score = 20;

        if (portfolio.positionsCount() <= 1) {
            score += 25;
        } else if (portfolio.positionsCount() <= 2) {
            score += 15;
        } else if (portfolio.positionsCount() <= 4) {
            score += 8;
        }

        if (portfolio.totalProfitLossPercent().compareTo(BigDecimal.valueOf(-20)) <= 0) {
            score += 25;
        } else if (portfolio.totalProfitLossPercent().compareTo(BigDecimal.valueOf(-10)) <= 0) {
            score += 15;
        } else if (portfolio.totalProfitLossPercent().compareTo(BigDecimal.ZERO) < 0) {
            score += 8;
        }

        long cryptoPositions = portfolio.positions()
                .stream()
                .filter(position -> "CRYPTO".equals(position.assetType().name()))
                .count();

        if (cryptoPositions == portfolio.positionsCount() && portfolio.positionsCount() > 0) {
            score += 20;
        } else if (cryptoPositions > 0) {
            score += 10;
        }

        return clampRiskScore(score);
    }

    private RiskLevel calculateRiskLevel(int riskScore) {
        if (riskScore >= 80) {
            return RiskLevel.CRITICAL;
        }

        if (riskScore >= 60) {
            return RiskLevel.HIGH;
        }

        if (riskScore >= 35) {
            return RiskLevel.MEDIUM;
        }

        return RiskLevel.LOW;
    }
}
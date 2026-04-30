package com.investnavigator.backend.ai.provider.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investnavigator.backend.ai.provider.dto.AIAnalysisResult;
import com.investnavigator.backend.ai.provider.dto.AICompareAnalysisRequest;
import com.investnavigator.backend.ai.provider.dto.AICompareAssetSnapshot;
import com.investnavigator.backend.ai.provider.dto.AIProviderJsonPayload;
import com.investnavigator.backend.ai.provider.dto.AIWatchlistAnalysisRequest;
import com.investnavigator.backend.analytics.dto.AnalyticsSummaryResponse;
import com.investnavigator.backend.analytics.model.RiskLevel;
import com.investnavigator.backend.portfolio.dto.PortfolioSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
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

    public AIAnalysisResult parseWatchlist(
            String rawText,
            AIWatchlistAnalysisRequest fallbackWatchlist
    ) {
        AIProviderJsonPayload payload = parsePayload(rawText);

        int fallbackRiskScore = calculateWatchlistFallbackRiskScore(fallbackWatchlist);
        RiskLevel fallbackRiskLevel = calculateRiskLevel(fallbackRiskScore);

        return new AIAnalysisResult(
                safeString(payload.summary(), buildWatchlistFallbackSummary(fallbackWatchlist)),
                safeList(payload.positiveFactors(), "No positive watchlist factors were provided by the AI provider."),
                safeList(payload.negativeFactors(), "No negative watchlist factors were provided by the AI provider."),
                payload.riskLevel() == null ? fallbackRiskLevel : payload.riskLevel(),
                payload.riskScore() == null ? fallbackRiskScore : clampRiskScore(payload.riskScore()),
                safeConfidence(payload.confidence()),
                safeString(payload.explanation(), buildWatchlistFallbackExplanation(fallbackWatchlist, fallbackRiskScore, fallbackRiskLevel)),
                safeString(payload.disclaimer(), DEFAULT_DISCLAIMER)
        );
    }

    public AIAnalysisResult parseCompare(
            String rawText,
            AICompareAnalysisRequest fallbackCompare
    ) {
        AIProviderJsonPayload payload = parsePayload(rawText);

        int fallbackRiskScore = calculateCompareFallbackRiskScore(fallbackCompare);
        RiskLevel fallbackRiskLevel = calculateRiskLevel(fallbackRiskScore);

        return new AIAnalysisResult(
                safeString(payload.summary(), buildCompareFallbackSummary(fallbackCompare)),
                safeList(payload.positiveFactors(), "No positive comparison factors were provided by the AI provider."),
                safeList(payload.negativeFactors(), "No negative comparison factors were provided by the AI provider."),
                payload.riskLevel() == null ? fallbackRiskLevel : payload.riskLevel(),
                payload.riskScore() == null ? fallbackRiskScore : clampRiskScore(payload.riskScore()),
                safeConfidence(payload.confidence()),
                safeString(payload.explanation(), buildCompareFallbackExplanation(fallbackCompare, fallbackRiskScore, fallbackRiskLevel)),
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

    private String buildWatchlistFallbackSummary(AIWatchlistAnalysisRequest watchlist) {
        return "Watchlist was analyzed using available market data.";
    }

    private String buildWatchlistFallbackExplanation(
            AIWatchlistAnalysisRequest watchlist,
            int riskScore,
            RiskLevel riskLevel
    ) {
        long cryptoItems = watchlist.items()
                .stream()
                .filter(item -> "CRYPTO".equals(item.assetType().name()))
                .count();

        long stockItems = watchlist.items()
                .stream()
                .filter(item -> "STOCK".equals(item.assetType().name()))
                .count();

        long itemsWithErrors = watchlist.items()
                .stream()
                .filter(item -> item.dataError() != null && !item.dataError().isBlank())
                .count();

        return """
                Fallback watchlist explanation generated because the AI provider response did not include a valid explanation.
                
                Items count: %s
                Stock items: %s
                Crypto items: %s
                Items with data errors: %s
                Risk score: %s / 100
                Risk level: %s
                """.formatted(
                watchlist.items().size(),
                stockItems,
                cryptoItems,
                itemsWithErrors,
                riskScore,
                riskLevel
        );
    }

    private String buildCompareFallbackSummary(AICompareAnalysisRequest compare) {
        String tickers = compare.assets()
                .stream()
                .map(AICompareAssetSnapshot::ticker)
                .reduce((first, second) -> first + " vs " + second)
                .orElse("assets");

        return "Comparison report was generated using available analytics metrics for " + tickers + ".";
    }

    private String buildCompareFallbackExplanation(
            AICompareAnalysisRequest compare,
            int riskScore,
            RiskLevel riskLevel
    ) {
        AICompareAssetSnapshot riskiestAsset = compare.assets()
                .stream()
                .max(Comparator.comparingInt(AICompareAssetSnapshot::riskScore))
                .orElse(null);

        AICompareAssetSnapshot mostVolatileAsset = compare.assets()
                .stream()
                .max(Comparator.comparing(AICompareAssetSnapshot::volatilityPercent))
                .orElse(null);

        String riskiestTicker = riskiestAsset == null ? "n/a" : riskiestAsset.ticker();
        String volatileTicker = mostVolatileAsset == null ? "n/a" : mostVolatileAsset.ticker();

        return """
                Fallback comparison explanation generated because the AI provider response did not include a valid explanation.
                
                Assets count: %s
                Riskiest asset by risk score: %s
                Most volatile asset: %s
                Comparison risk score: %s / 100
                Comparison risk level: %s
                """.formatted(
                compare.assets().size(),
                riskiestTicker,
                volatileTicker,
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

    private int calculateWatchlistFallbackRiskScore(AIWatchlistAnalysisRequest watchlist) {
        int score = 20;

        if (watchlist.items().size() <= 1) {
            score += 20;
        } else if (watchlist.items().size() <= 2) {
            score += 12;
        }

        long cryptoItems = watchlist.items()
                .stream()
                .filter(item -> "CRYPTO".equals(item.assetType().name()))
                .count();

        if (cryptoItems == watchlist.items().size() && !watchlist.items().isEmpty()) {
            score += 20;
        } else if (cryptoItems > 0) {
            score += 10;
        }

        long itemsWithErrors = watchlist.items()
                .stream()
                .filter(item -> item.dataError() != null && !item.dataError().isBlank())
                .count();

        score += (int) Math.min(itemsWithErrors * 8, 24);

        return clampRiskScore(score);
    }

    private int calculateCompareFallbackRiskScore(AICompareAnalysisRequest compare) {
        int maxRiskScore = compare.assets()
                .stream()
                .mapToInt(AICompareAssetSnapshot::riskScore)
                .max()
                .orElse(20);

        int lowDataPenalty = compare.assets()
                .stream()
                .anyMatch(asset -> asset.dataPoints() < 5)
                ? 10
                : 0;

        return clampRiskScore(maxRiskScore + lowDataPenalty);
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
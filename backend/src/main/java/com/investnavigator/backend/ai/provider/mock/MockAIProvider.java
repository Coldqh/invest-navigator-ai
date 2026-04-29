package com.investnavigator.backend.ai.provider.mock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investnavigator.backend.ai.provider.AIProvider;
import com.investnavigator.backend.ai.provider.AIProviderType;
import com.investnavigator.backend.ai.provider.dto.AIAnalysisRequest;
import com.investnavigator.backend.ai.provider.dto.AIAnalysisResult;
import com.investnavigator.backend.ai.provider.dto.AIPortfolioAnalysisRequest;
import com.investnavigator.backend.ai.provider.dto.AIProviderJsonPayload;
import com.investnavigator.backend.ai.provider.dto.AIProviderPrompt;
import com.investnavigator.backend.ai.provider.dto.AIProviderRawResponse;
import com.investnavigator.backend.ai.provider.parser.AIProviderJsonParser;
import com.investnavigator.backend.ai.provider.prompt.AIProviderPromptBuilder;
import com.investnavigator.backend.analytics.dto.AnalyticsSummaryResponse;
import com.investnavigator.backend.analytics.model.RiskLevel;
import com.investnavigator.backend.portfolio.dto.PortfolioPositionResponse;
import com.investnavigator.backend.portfolio.dto.PortfolioSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MockAIProvider implements AIProvider {

    private static final int CONFIDENCE_SCALE = 2;
    private static final String MOCK_MODEL_NAME = "mock-rules-v1";

    private static final String DISCLAIMER = """
            This report is generated for educational purposes only.
            It is not financial advice and should not be used as the only basis for investment decisions.
            """;

    private final AIProviderPromptBuilder promptBuilder;
    private final AIProviderJsonParser jsonParser;
    private final ObjectMapper objectMapper;

    @Override
    public AIProviderType getType() {
        return AIProviderType.MOCK;
    }

    @Override
    public AIAnalysisResult analyze(AIAnalysisRequest request) {
        AnalyticsSummaryResponse analytics = request.analytics();
        AIProviderPrompt prompt = promptBuilder.buildPrompt(request);

        AIProviderJsonPayload payload = new AIProviderJsonPayload(
                buildSummary(analytics),
                buildPositiveFactors(analytics),
                buildNegativeFactors(analytics),
                analytics.riskLevel(),
                analytics.riskScore(),
                calculateConfidence(analytics),
                buildExplanation(analytics, prompt),
                DISCLAIMER
        );

        AIProviderRawResponse rawResponse = new AIProviderRawResponse(
                getType(),
                MOCK_MODEL_NAME,
                toJson(payload),
                Instant.now()
        );

        return jsonParser.parse(rawResponse.rawText(), analytics);
    }

    @Override
    public AIAnalysisResult analyzePortfolio(AIPortfolioAnalysisRequest request) {
        PortfolioSummaryResponse portfolio = request.portfolio();
        AIProviderPrompt prompt = promptBuilder.buildPortfolioPrompt(request);

        int riskScore = calculatePortfolioRiskScore(portfolio);
        RiskLevel riskLevel = calculateRiskLevel(riskScore);

        AIProviderJsonPayload payload = new AIProviderJsonPayload(
                buildPortfolioSummary(portfolio, riskLevel),
                buildPortfolioPositiveFactors(portfolio),
                buildPortfolioNegativeFactors(portfolio, riskLevel),
                riskLevel,
                riskScore,
                calculatePortfolioConfidence(portfolio),
                buildPortfolioExplanation(portfolio, prompt, riskScore, riskLevel),
                DISCLAIMER
        );

        AIProviderRawResponse rawResponse = new AIProviderRawResponse(
                getType(),
                MOCK_MODEL_NAME,
                toJson(payload),
                Instant.now()
        );

        return jsonParser.parsePortfolio(rawResponse.rawText(), portfolio);
    }

    private String buildSummary(AnalyticsSummaryResponse analytics) {
        if (analytics.riskLevel() == RiskLevel.CRITICAL) {
            return analytics.ticker() + " shows extremely high risk based on volatility and price movement.";
        }

        if (analytics.riskLevel() == RiskLevel.HIGH) {
            return analytics.ticker() + " has elevated risk and requires careful monitoring.";
        }

        if (analytics.riskLevel() == RiskLevel.MEDIUM) {
            return analytics.ticker() + " has moderate risk with mixed market signals.";
        }

        return analytics.ticker() + " currently looks relatively stable based on available market data.";
    }

    private List<String> buildPositiveFactors(AnalyticsSummaryResponse analytics) {
        List<String> factors = new ArrayList<>();

        if (analytics.priceChangePercent().compareTo(BigDecimal.ZERO) > 0) {
            factors.add("Positive price change over the analyzed candle period.");
        }

        if (analytics.dataPoints() >= 10) {
            factors.add("The analysis is based on a stronger data sample.");
        } else if (analytics.dataPoints() >= 3) {
            factors.add("The analysis has enough data points for a basic MVP-level estimate.");
        }

        if (analytics.riskLevel() == RiskLevel.LOW || analytics.riskLevel() == RiskLevel.MEDIUM) {
            factors.add("Risk level is not in the high-risk zone.");
        }

        if (factors.isEmpty()) {
            factors.add("No strong positive factors were detected in the current dataset.");
        }

        return factors;
    }

    private List<String> buildNegativeFactors(AnalyticsSummaryResponse analytics) {
        List<String> factors = new ArrayList<>();

        if (analytics.priceChangePercent().compareTo(BigDecimal.ZERO) < 0) {
            factors.add("Negative price movement over the analyzed candle period.");
        }

        if (analytics.volatilityPercent().compareTo(BigDecimal.valueOf(5)) > 0) {
            factors.add("High volatility may increase short-term uncertainty.");
        }

        if (analytics.riskLevel() == RiskLevel.HIGH || analytics.riskLevel() == RiskLevel.CRITICAL) {
            factors.add("Risk score is high compared with safer assets in the dataset.");
        }

        if (analytics.dataPoints() < 5) {
            factors.add("Limited data sample reduces confidence in the analysis.");
        }

        if (factors.isEmpty()) {
            factors.add("No major negative factors were detected in the current dataset.");
        }

        return factors;
    }

    private BigDecimal calculateConfidence(AnalyticsSummaryResponse analytics) {
        BigDecimal confidence = BigDecimal.valueOf(0.55);

        if (analytics.dataPoints() >= 30) {
            confidence = confidence.add(BigDecimal.valueOf(0.25));
        } else if (analytics.dataPoints() >= 10) {
            confidence = confidence.add(BigDecimal.valueOf(0.18));
        } else if (analytics.dataPoints() >= 5) {
            confidence = confidence.add(BigDecimal.valueOf(0.10));
        }

        if (analytics.riskLevel() == RiskLevel.LOW || analytics.riskLevel() == RiskLevel.MEDIUM) {
            confidence = confidence.add(BigDecimal.valueOf(0.05));
        }

        BigDecimal maxConfidence = BigDecimal.valueOf(0.95);

        if (confidence.compareTo(maxConfidence) > 0) {
            confidence = maxConfidence;
        }

        return confidence.setScale(CONFIDENCE_SCALE, RoundingMode.HALF_UP);
    }

    private String buildExplanation(
            AnalyticsSummaryResponse analytics,
            AIProviderPrompt prompt
    ) {
        return """
                The mock AI provider analyzed the available market metrics using the same prompt structure that real AI providers will use later.
                
                Prepared system prompt length: %s characters
                Prepared user prompt length: %s characters
                
                Market metrics:
                - Current price: %s
                - First close: %s
                - Last close: %s
                - Price change: %s
                - Price change percent: %s%%
                - Average volume: %s
                - Volatility: %s%%
                - Risk score: %s / 100
                - Risk level: %s
                - Data points: %s
                
                This explanation is deterministic and generated by internal business rules.
                Later it can be replaced by a real AI provider without changing the report API.
                """.formatted(
                prompt.systemPrompt().length(),
                prompt.userPrompt().length(),
                analytics.currentPrice(),
                analytics.firstClose(),
                analytics.lastClose(),
                analytics.priceChange(),
                analytics.priceChangePercent(),
                analytics.averageVolume(),
                analytics.volatilityPercent(),
                analytics.riskScore(),
                analytics.riskLevel(),
                analytics.dataPoints()
        );
    }

    private String buildPortfolioSummary(
            PortfolioSummaryResponse portfolio,
            RiskLevel riskLevel
    ) {
        if (portfolio.totalProfitLoss().compareTo(BigDecimal.ZERO) > 0) {
            return "Portfolio is currently profitable with " + riskLevel + " risk based on current allocation.";
        }

        if (portfolio.totalProfitLoss().compareTo(BigDecimal.ZERO) < 0) {
            return "Portfolio is currently in loss with " + riskLevel + " risk based on current allocation.";
        }

        return "Portfolio is close to breakeven with " + riskLevel + " risk based on current allocation.";
    }

    private List<String> buildPortfolioPositiveFactors(PortfolioSummaryResponse portfolio) {
        List<String> factors = new ArrayList<>();

        if (portfolio.positionsCount() >= 3) {
            factors.add("Portfolio has several positions instead of depending on only one asset.");
        }

        if (portfolio.totalProfitLoss().compareTo(BigDecimal.ZERO) > 0) {
            factors.add("Total portfolio profit/loss is currently positive.");
        }

        boolean hasStock = portfolio.positions()
                .stream()
                .anyMatch(position -> "STOCK".equals(position.assetType().name()));

        boolean hasCrypto = portfolio.positions()
                .stream()
                .anyMatch(position -> "CRYPTO".equals(position.assetType().name()));

        if (hasStock && hasCrypto) {
            factors.add("Portfolio contains both stocks and crypto assets.");
        }

        if (factors.isEmpty()) {
            factors.add("No strong positive portfolio factors were detected.");
        }

        return factors;
    }

    private List<String> buildPortfolioNegativeFactors(
            PortfolioSummaryResponse portfolio,
            RiskLevel riskLevel
    ) {
        List<String> factors = new ArrayList<>();

        if (portfolio.positionsCount() <= 1) {
            factors.add("Portfolio is highly concentrated in a single position.");
        }

        if (portfolio.totalProfitLoss().compareTo(BigDecimal.ZERO) < 0) {
            factors.add("Total portfolio profit/loss is currently negative.");
        }

        long cryptoPositions = portfolio.positions()
                .stream()
                .filter(position -> "CRYPTO".equals(position.assetType().name()))
                .count();

        if (cryptoPositions == portfolio.positionsCount() && portfolio.positionsCount() > 0) {
            factors.add("Portfolio consists only of crypto assets, which may increase volatility.");
        }

        PortfolioPositionResponse worstPosition = portfolio.positions()
                .stream()
                .min((first, second) -> first.profitLossPercent().compareTo(second.profitLossPercent()))
                .orElse(null);

        if (worstPosition != null && worstPosition.profitLossPercent().compareTo(BigDecimal.ZERO) < 0) {
            factors.add("Weakest position by profit/loss percent: " + worstPosition.ticker() + ".");
        }

        if (riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL) {
            factors.add("Overall portfolio risk is in the high-risk zone.");
        }

        if (factors.isEmpty()) {
            factors.add("No major negative portfolio factors were detected.");
        }

        return factors;
    }

    private int calculatePortfolioRiskScore(PortfolioSummaryResponse portfolio) {
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

        return Math.min(score, 100);
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

    private BigDecimal calculatePortfolioConfidence(PortfolioSummaryResponse portfolio) {
        BigDecimal confidence = BigDecimal.valueOf(0.55);

        if (portfolio.positionsCount() >= 5) {
            confidence = confidence.add(BigDecimal.valueOf(0.20));
        } else if (portfolio.positionsCount() >= 3) {
            confidence = confidence.add(BigDecimal.valueOf(0.12));
        } else if (portfolio.positionsCount() >= 1) {
            confidence = confidence.add(BigDecimal.valueOf(0.05));
        }

        BigDecimal maxConfidence = BigDecimal.valueOf(0.90);

        if (confidence.compareTo(maxConfidence) > 0) {
            confidence = maxConfidence;
        }

        return confidence.setScale(CONFIDENCE_SCALE, RoundingMode.HALF_UP);
    }

    private String buildPortfolioExplanation(
            PortfolioSummaryResponse portfolio,
            AIProviderPrompt prompt,
            int riskScore,
            RiskLevel riskLevel
    ) {
        return """
                The mock AI provider analyzed the current portfolio using the same prompt structure that real AI providers use.
                
                Prepared system prompt length: %s characters
                Prepared user prompt length: %s characters
                
                Portfolio metrics:
                - Positions count: %s
                - Total invested: %s
                - Total current value: %s
                - Total profit/loss: %s
                - Total profit/loss percent: %s%%
                - Risk score: %s / 100
                - Risk level: %s
                
                This explanation is deterministic and generated by internal business rules.
                Later it can be replaced by a real AI provider without changing the portfolio report API.
                """.formatted(
                prompt.systemPrompt().length(),
                prompt.userPrompt().length(),
                portfolio.positionsCount(),
                portfolio.totalInvested(),
                portfolio.totalCurrentValue(),
                portfolio.totalProfitLoss(),
                portfolio.totalProfitLossPercent(),
                riskScore,
                riskLevel
        );
    }

    private String toJson(AIProviderJsonPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize mock AI provider payload",
                    exception
            );
        }
    }
}
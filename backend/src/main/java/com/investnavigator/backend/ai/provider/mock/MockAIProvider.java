package com.investnavigator.backend.ai.provider.mock;

import com.investnavigator.backend.ai.provider.AIProvider;
import com.investnavigator.backend.ai.provider.AIProviderType;
import com.investnavigator.backend.ai.provider.dto.AIAnalysisRequest;
import com.investnavigator.backend.ai.provider.dto.AIAnalysisResult;
import com.investnavigator.backend.analytics.dto.AnalyticsSummaryResponse;
import com.investnavigator.backend.analytics.model.RiskLevel;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class MockAIProvider implements AIProvider {

    private static final int CONFIDENCE_SCALE = 2;

    private static final String DISCLAIMER = """
            This report is generated for educational purposes only.
            It is not financial advice and should not be used as the only basis for investment decisions.
            """;

    @Override
    public AIProviderType getType() {
        return AIProviderType.MOCK;
    }

    @Override
    public AIAnalysisResult analyze(AIAnalysisRequest request) {
        AnalyticsSummaryResponse analytics = request.analytics();

        return new AIAnalysisResult(
                buildSummary(analytics),
                buildPositiveFactors(analytics),
                buildNegativeFactors(analytics),
                analytics.riskLevel(),
                analytics.riskScore(),
                calculateConfidence(analytics),
                buildExplanation(analytics),
                DISCLAIMER
        );
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

    private String buildExplanation(AnalyticsSummaryResponse analytics) {
        return """
                The mock AI provider analyzed the available market metrics:
                
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
}
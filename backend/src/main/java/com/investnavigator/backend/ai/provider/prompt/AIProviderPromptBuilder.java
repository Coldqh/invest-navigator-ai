package com.investnavigator.backend.ai.provider.prompt;

import com.investnavigator.backend.ai.provider.dto.AIAnalysisRequest;
import com.investnavigator.backend.ai.provider.dto.AIProviderPrompt;
import com.investnavigator.backend.analytics.dto.AnalyticsSummaryResponse;
import org.springframework.stereotype.Service;

@Service
public class AIProviderPromptBuilder {

    public AIProviderPrompt buildPrompt(AIAnalysisRequest request) {
        return new AIProviderPrompt(
                buildSystemPrompt(),
                buildUserPrompt(request)
        );
    }

    private String buildSystemPrompt() {
        return """
                You are an investment analytics assistant inside an educational portfolio analysis application.
                
                Your task:
                - analyze market metrics;
                - explain risk clearly;
                - return only valid JSON;
                - never provide direct financial advice;
                - always include a disclaimer.
                
                Required JSON schema:
                {
                  "summary": "string",
                  "positiveFactors": ["string"],
                  "negativeFactors": ["string"],
                  "riskLevel": "LOW | MEDIUM | HIGH | CRITICAL",
                  "riskScore": 0,
                  "confidence": 0.0,
                  "explanation": "string",
                  "disclaimer": "string"
                }
                """;
    }

    private String buildUserPrompt(AIAnalysisRequest request) {
        AnalyticsSummaryResponse analytics = request.analytics();

        return """
                Analyze this asset using the provided metrics.
                
                Asset:
                - Ticker: %s
                - Name: %s
                
                Market metrics:
                - Current price: %s
                - First close: %s
                - Last close: %s
                - Price change: %s
                - Price change percent: %s%%
                - Average volume: %s
                - Volatility percent: %s%%
                - Risk score: %s / 100
                - Risk level: %s
                - Data points: %s
                
                Return only JSON matching the required schema.
                """.formatted(
                request.ticker(),
                request.name(),
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
package com.investnavigator.backend.ai.provider.prompt;

import com.investnavigator.backend.ai.provider.dto.AIAnalysisRequest;
import com.investnavigator.backend.ai.provider.dto.AICompareAnalysisRequest;
import com.investnavigator.backend.ai.provider.dto.AICompareAssetSnapshot;
import com.investnavigator.backend.ai.provider.dto.AIPortfolioAnalysisRequest;
import com.investnavigator.backend.ai.provider.dto.AIProviderPrompt;
import com.investnavigator.backend.ai.provider.dto.AIWatchlistAnalysisRequest;
import com.investnavigator.backend.ai.provider.dto.AIWatchlistItemSnapshot;
import com.investnavigator.backend.analytics.dto.AnalyticsSummaryResponse;
import com.investnavigator.backend.portfolio.dto.PortfolioPositionResponse;
import com.investnavigator.backend.portfolio.dto.PortfolioSummaryResponse;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class AIProviderPromptBuilder {

    public AIProviderPrompt buildPrompt(AIAnalysisRequest request) {
        return new AIProviderPrompt(
                buildSystemPrompt(),
                buildUserPrompt(request)
        );
    }

    public AIProviderPrompt buildPortfolioPrompt(AIPortfolioAnalysisRequest request) {
        return new AIProviderPrompt(
                buildSystemPrompt(),
                buildPortfolioUserPrompt(request)
        );
    }

    public AIProviderPrompt buildWatchlistPrompt(AIWatchlistAnalysisRequest request) {
        return new AIProviderPrompt(
                buildSystemPrompt(),
                buildWatchlistUserPrompt(request)
        );
    }

    public AIProviderPrompt buildComparePrompt(AICompareAnalysisRequest request) {
        return new AIProviderPrompt(
                buildSystemPrompt(),
                buildCompareUserPrompt(request)
        );
    }

    private String buildSystemPrompt() {
        return """
                You are an investment analytics assistant inside an educational portfolio analysis application.
                
                Return only a valid JSON object.
                Do not use markdown.
                Do not use code fences.
                Do not add text before JSON.
                Do not add text after JSON.
                
                Required JSON object:
                {
                  "summary": "string",
                  "positiveFactors": ["string"],
                  "negativeFactors": ["string"],
                  "riskLevel": "LOW",
                  "riskScore": 0,
                  "confidence": 0.0,
                  "explanation": "string",
                  "disclaimer": "string"
                }
                
                Strict rules:
                - summary must be a short text;
                - positiveFactors must be an array of strings;
                - negativeFactors must be an array of strings;
                - riskLevel must be exactly one of: LOW, MEDIUM, HIGH, CRITICAL;
                - riskScore must be an integer from 0 to 100;
                - confidence must be a decimal number from 0 to 1;
                - explanation must explain the reasoning in plain language;
                - disclaimer must say that this is educational analysis and not financial advice;
                - never provide direct financial advice.
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
                
                Return only JSON matching the required object.
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

    private String buildPortfolioUserPrompt(AIPortfolioAnalysisRequest request) {
        PortfolioSummaryResponse portfolio = request.portfolio();

        String positionsText = portfolio.positions()
                .stream()
                .map(this::formatPortfolioPosition)
                .collect(Collectors.joining("\n"));

        return """
                Analyze this investment portfolio using the provided metrics.
                
                Portfolio totals:
                - Positions count: %s
                - Total invested: %s
                - Total current value: %s
                - Total profit/loss: %s
                - Total profit/loss percent: %s%%
                - Calculated at: %s
                
                Portfolio positions:
                %s
                
                Focus on:
                - portfolio concentration;
                - profit and loss situation;
                - risky positions;
                - balance between crypto and stocks;
                - whether the portfolio looks stable or speculative.
                
                Return only JSON matching the required object.
                """.formatted(
                portfolio.positionsCount(),
                portfolio.totalInvested(),
                portfolio.totalCurrentValue(),
                portfolio.totalProfitLoss(),
                portfolio.totalProfitLossPercent(),
                portfolio.calculatedAt(),
                positionsText
        );
    }

    private String buildWatchlistUserPrompt(AIWatchlistAnalysisRequest request) {
        String itemsText = request.items()
                .stream()
                .map(this::formatWatchlistItem)
                .collect(Collectors.joining("\n"));

        return """
                Analyze this watchlist using the provided market data.
                
                Watchlist:
                - Items count: %s
                - Generated at: %s
                
                Watchlist items:
                %s
                
                Focus on:
                - assets that look risky;
                - assets that look relatively stable;
                - balance between crypto and stocks;
                - data source quality;
                - what deserves closer monitoring.
                
                Return only JSON matching the required object.
                """.formatted(
                request.items().size(),
                request.generatedAt(),
                itemsText
        );
    }

    private String buildCompareUserPrompt(AICompareAnalysisRequest request) {
        String assetsText = request.assets()
                .stream()
                .map(this::formatCompareAsset)
                .collect(Collectors.joining("\n"));

        return """
                Compare these assets using the provided analytics metrics.
                
                Compare request:
                - Assets count: %s
                - Generated at: %s
                
                Assets:
                %s
                
                Focus on:
                - which asset looks more volatile;
                - which asset has stronger price movement;
                - which asset has better risk profile;
                - whether one asset looks more speculative;
                - data quality and sample size;
                - clear explanation for a beginner investor.
                
                Do not tell the user to buy or sell.
                Return only JSON matching the required object.
                """.formatted(
                request.assets().size(),
                request.generatedAt(),
                assetsText
        );
    }

    private String formatPortfolioPosition(PortfolioPositionResponse position) {
        return """
                - %s:
                  name: %s
                  type: %s
                  exchange: %s
                  currency: %s
                  quantity: %s
                  average buy price: %s
                  invested amount: %s
                  current price: %s
                  current value: %s
                  profit/loss: %s
                  profit/loss percent: %s%%
                  price source: %s
                """.formatted(
                position.ticker(),
                position.name(),
                position.assetType(),
                position.exchange(),
                position.currency(),
                position.quantity(),
                position.averageBuyPrice(),
                position.investedAmount(),
                position.currentPrice(),
                position.currentValue(),
                position.profitLoss(),
                position.profitLossPercent(),
                position.priceSource()
        );
    }

    private String formatWatchlistItem(AIWatchlistItemSnapshot item) {
        return """
                - %s:
                  name: %s
                  type: %s
                  exchange: %s
                  currency: %s
                  latest price: %s
                  latest volume: %s
                  price source: %s
                  price timestamp: %s
                  data error: %s
                """.formatted(
                item.ticker(),
                item.name(),
                item.assetType(),
                item.exchange(),
                item.currency(),
                item.latestPrice(),
                item.latestVolume(),
                item.priceSource(),
                item.priceTimestamp(),
                item.dataError()
        );
    }

    private String formatCompareAsset(AICompareAssetSnapshot asset) {
        return """
                - %s:
                  name: %s
                  current price: %s
                  first close: %s
                  last close: %s
                  price change: %s
                  price change percent: %s%%
                  average volume: %s
                  volatility percent: %s%%
                  risk score: %s / 100
                  risk level: %s
                  data points: %s
                """.formatted(
                asset.ticker(),
                asset.name(),
                asset.currentPrice(),
                asset.firstClose(),
                asset.lastClose(),
                asset.priceChange(),
                asset.priceChangePercent(),
                asset.averageVolume(),
                asset.volatilityPercent(),
                asset.riskScore(),
                asset.riskLevel(),
                asset.dataPoints()
        );
    }
}
package com.investnavigator.backend.ai.service;

import com.investnavigator.backend.ai.config.AIProperties;
import com.investnavigator.backend.ai.dto.AIWatchlistReportResponse;
import com.investnavigator.backend.ai.provider.AIProvider;
import com.investnavigator.backend.ai.provider.AIProviderRegistry;
import com.investnavigator.backend.ai.provider.AIProviderType;
import com.investnavigator.backend.ai.provider.dto.AIAnalysisResult;
import com.investnavigator.backend.ai.provider.dto.AIWatchlistAnalysisRequest;
import com.investnavigator.backend.ai.provider.dto.AIWatchlistItemSnapshot;
import com.investnavigator.backend.common.error.BadRequestException;
import com.investnavigator.backend.marketdata.dto.MarketPriceResponse;
import com.investnavigator.backend.marketdata.service.MarketDataService;
import com.investnavigator.backend.watchlist.dto.WatchlistItemResponse;
import com.investnavigator.backend.watchlist.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AIWatchlistReportService {

    private static final Logger log = LoggerFactory.getLogger(AIWatchlistReportService.class);

    private final WatchlistService watchlistService;
    private final MarketDataService marketDataService;
    private final AIProperties aiProperties;
    private final AIProviderRegistry aiProviderRegistry;

    public AIWatchlistReportResponse generateWatchlistReport() {
        List<WatchlistItemResponse> watchlist = watchlistService.getWatchlist();

        if (watchlist.isEmpty()) {
            throw new BadRequestException("Watchlist is empty. Add at least one asset before generating AI watchlist report.");
        }

        AIWatchlistAnalysisRequest request = new AIWatchlistAnalysisRequest(
                buildWatchlistSnapshots(watchlist),
                Instant.now()
        );

        AnalysisWithProvider analysisWithProvider = analyzeWithFallback(request);
        AIAnalysisResult analysisResult = analysisWithProvider.analysisResult();

        return new AIWatchlistReportResponse(
                analysisWithProvider.providerType(),
                request.items().size(),
                request.items(),
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

    private List<AIWatchlistItemSnapshot> buildWatchlistSnapshots(
            List<WatchlistItemResponse> watchlist
    ) {
        return watchlist.stream()
                .map(this::toSnapshot)
                .toList();
    }

    private AIWatchlistItemSnapshot toSnapshot(WatchlistItemResponse item) {
        try {
            MarketPriceResponse price = marketDataService.getLatestMarketPrice(item.ticker());

            return new AIWatchlistItemSnapshot(
                    item.ticker(),
                    item.name(),
                    item.assetType(),
                    item.exchange(),
                    item.currency(),
                    price.price(),
                    price.volume(),
                    price.source(),
                    price.timestamp(),
                    null
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to load market data for watchlist item {}. It will be included with dataError. Reason: {}",
                    item.ticker(),
                    exception.getMessage()
            );

            return new AIWatchlistItemSnapshot(
                    item.ticker(),
                    item.name(),
                    item.assetType(),
                    item.exchange(),
                    item.currency(),
                    null,
                    null,
                    null,
                    null,
                    exception.getMessage()
            );
        }
    }

    private AnalysisWithProvider analyzeWithFallback(AIWatchlistAnalysisRequest request) {
        AIProviderType activeProviderType = aiProperties.provider();
        AIProvider activeProvider = aiProviderRegistry.getProvider(activeProviderType);

        try {
            AIAnalysisResult analysisResult = activeProvider.analyzeWatchlist(request);

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
                    "AI provider {} failed for watchlist report. Falling back to MOCK provider. Reason: {}",
                    activeProviderType,
                    fallbackReason
            );

            AIProvider mockProvider = aiProviderRegistry.getProvider(AIProviderType.MOCK);

            return new AnalysisWithProvider(
                    AIProviderType.MOCK,
                    mockProvider.analyzeWatchlist(request),
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
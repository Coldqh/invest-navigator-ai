package com.investnavigator.backend.marketdata.service;

import com.investnavigator.backend.asset.model.Asset;
import com.investnavigator.backend.asset.repository.AssetRepository;
import com.investnavigator.backend.marketdata.config.MarketDataProperties;
import com.investnavigator.backend.marketdata.dto.MarketDataProviderHealthItemResponse;
import com.investnavigator.backend.marketdata.dto.MarketDataProviderHealthResponse;
import com.investnavigator.backend.marketdata.dto.ProviderHealthStatus;
import com.investnavigator.backend.marketdata.provider.MarketDataProvider;
import com.investnavigator.backend.marketdata.provider.MarketDataProviderRegistry;
import com.investnavigator.backend.marketdata.provider.MarketDataProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketDataProviderHealthService {

    private static final String BINANCE_TEST_TICKER = "BTCUSDT";

    private final MarketDataProperties marketDataProperties;
    private final MarketDataProviderRegistry marketDataProviderRegistry;
    private final AssetRepository assetRepository;

    public MarketDataProviderHealthResponse getHealth() {
        Instant checkedAt = Instant.now();

        List<MarketDataProviderHealthItemResponse> providers = new ArrayList<>();
        providers.add(checkDemoProvider(checkedAt));
        providers.add(checkBinanceProvider(checkedAt));
        providers.add(notConfigured(MarketDataProviderType.MOEX, "MOEX provider is not implemented yet", checkedAt));
        providers.add(notConfigured(MarketDataProviderType.T_INVEST, "T-Invest provider is not implemented yet", checkedAt));

        ProviderHealthStatus overallStatus = calculateOverallStatus(providers);

        return new MarketDataProviderHealthResponse(
                marketDataProperties.provider(),
                overallStatus,
                providers,
                checkedAt
        );
    }

    private MarketDataProviderHealthItemResponse checkDemoProvider(Instant checkedAt) {
        try {
            marketDataProviderRegistry.getProvider(MarketDataProviderType.DEMO);

            return new MarketDataProviderHealthItemResponse(
                    MarketDataProviderType.DEMO,
                    ProviderHealthStatus.AVAILABLE,
                    "Demo provider is available",
                    checkedAt
            );
        } catch (RuntimeException exception) {
            return new MarketDataProviderHealthItemResponse(
                    MarketDataProviderType.DEMO,
                    ProviderHealthStatus.UNAVAILABLE,
                    "Demo provider is unavailable: " + exception.getMessage(),
                    checkedAt
            );
        }
    }

    private MarketDataProviderHealthItemResponse checkBinanceProvider(Instant checkedAt) {
        try {
            MarketDataProvider binanceProvider = marketDataProviderRegistry.getProvider(
                    MarketDataProviderType.BINANCE
            );

            Asset testAsset = assetRepository.findByTickerIgnoreCase(BINANCE_TEST_TICKER)
                    .orElse(null);

            if (testAsset == null) {
                return new MarketDataProviderHealthItemResponse(
                        MarketDataProviderType.BINANCE,
                        ProviderHealthStatus.NOT_CONFIGURED,
                        "BTCUSDT asset not found, Binance health check cannot be completed",
                        checkedAt
                );
            }

            binanceProvider.getLatestMarketPrice(testAsset);

            return new MarketDataProviderHealthItemResponse(
                    MarketDataProviderType.BINANCE,
                    ProviderHealthStatus.AVAILABLE,
                    "Binance provider responded successfully",
                    checkedAt
            );
        } catch (RestClientException exception) {
            return new MarketDataProviderHealthItemResponse(
                    MarketDataProviderType.BINANCE,
                    ProviderHealthStatus.UNAVAILABLE,
                    "Binance request failed: " + exception.getMessage(),
                    checkedAt
            );
        } catch (RuntimeException exception) {
            return new MarketDataProviderHealthItemResponse(
                    MarketDataProviderType.BINANCE,
                    ProviderHealthStatus.UNAVAILABLE,
                    "Binance provider is unavailable: " + exception.getMessage(),
                    checkedAt
            );
        }
    }

    private MarketDataProviderHealthItemResponse notConfigured(
            MarketDataProviderType type,
            String message,
            Instant checkedAt
    ) {
        return new MarketDataProviderHealthItemResponse(
                type,
                ProviderHealthStatus.NOT_CONFIGURED,
                message,
                checkedAt
        );
    }

    private ProviderHealthStatus calculateOverallStatus(
            List<MarketDataProviderHealthItemResponse> providers
    ) {
        ProviderHealthStatus demoStatus = findStatus(providers, MarketDataProviderType.DEMO);
        ProviderHealthStatus binanceStatus = findStatus(providers, MarketDataProviderType.BINANCE);

        if (marketDataProperties.provider() == MarketDataProviderType.DEMO) {
            return demoStatus;
        }

        if (marketDataProperties.provider() == MarketDataProviderType.BINANCE) {
            return binanceStatus;
        }

        if (marketDataProperties.provider() == MarketDataProviderType.HYBRID) {
            if (demoStatus == ProviderHealthStatus.AVAILABLE
                    && binanceStatus == ProviderHealthStatus.AVAILABLE) {
                return ProviderHealthStatus.AVAILABLE;
            }

            if (demoStatus == ProviderHealthStatus.AVAILABLE) {
                return ProviderHealthStatus.DEGRADED;
            }

            return ProviderHealthStatus.UNAVAILABLE;
        }

        return ProviderHealthStatus.NOT_CONFIGURED;
    }

    private ProviderHealthStatus findStatus(
            List<MarketDataProviderHealthItemResponse> providers,
            MarketDataProviderType type
    ) {
        return providers.stream()
                .filter(provider -> provider.type() == type)
                .map(MarketDataProviderHealthItemResponse::status)
                .findFirst()
                .orElse(ProviderHealthStatus.NOT_CONFIGURED);
    }
}
package com.investnavigator.backend.marketdata.service;

import com.investnavigator.backend.asset.model.Asset;
import com.investnavigator.backend.asset.model.AssetType;
import com.investnavigator.backend.asset.repository.AssetRepository;
import com.investnavigator.backend.common.error.BadRequestException;
import com.investnavigator.backend.common.error.ResourceNotFoundException;
import com.investnavigator.backend.marketdata.config.MarketDataProperties;
import com.investnavigator.backend.marketdata.dto.MarketPriceResponse;
import com.investnavigator.backend.marketdata.mapper.MarketDataMapper;
import com.investnavigator.backend.marketdata.model.MarketPrice;
import com.investnavigator.backend.marketdata.provider.MarketDataProvider;
import com.investnavigator.backend.marketdata.provider.MarketDataProviderRegistry;
import com.investnavigator.backend.marketdata.provider.MarketDataProviderType;
import com.investnavigator.backend.marketdata.repository.MarketPriceRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class MarketDataRefreshService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataRefreshService.class);

    private static final int SCALE = 6;

    private final AssetRepository assetRepository;
    private final MarketPriceRepository marketPriceRepository;
    private final MarketDataMapper marketDataMapper;
    private final MarketDataProperties marketDataProperties;
    private final MarketDataProviderRegistry marketDataProviderRegistry;

    @Transactional
    public MarketPriceResponse refreshLatestPrice(String ticker) {
        Asset asset = assetRepository.findByTickerIgnoreCase(ticker)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + ticker));

        MarketDataProviderType activeProvider = marketDataProperties.provider();

        if (activeProvider == MarketDataProviderType.HYBRID) {
            return refreshHybridPrice(asset);
        }

        if (activeProvider == MarketDataProviderType.BINANCE) {
            validateAssetType(asset, AssetType.CRYPTO, "BINANCE refresh supports only crypto assets");

            return refreshExternalPriceWithFallback(
                    asset,
                    MarketDataProviderType.BINANCE,
                    "BINANCE_REFRESH"
            );
        }

        if (activeProvider == MarketDataProviderType.MOEX) {
            validateAssetType(asset, AssetType.STOCK, "MOEX refresh supports only stock assets");

            return refreshExternalPriceWithFallback(
                    asset,
                    MarketDataProviderType.MOEX,
                    "MOEX_REFRESH"
            );
        }

        if (activeProvider == MarketDataProviderType.DEMO) {
            return refreshDemoPrice(asset);
        }

        throw new BadRequestException(
                "Manual refresh is not available for market data provider: " + activeProvider
        );
    }

    private MarketPriceResponse refreshHybridPrice(Asset asset) {
        if (asset.getAssetType() == AssetType.CRYPTO) {
            return refreshExternalPriceWithFallback(
                    asset,
                    MarketDataProviderType.BINANCE,
                    "BINANCE_REFRESH"
            );
        }

        if (asset.getAssetType() == AssetType.STOCK) {
            return refreshExternalPriceWithFallback(
                    asset,
                    MarketDataProviderType.MOEX,
                    "MOEX_REFRESH"
            );
        }

        return refreshDemoPrice(asset);
    }

    private MarketPriceResponse refreshExternalPriceWithFallback(
            Asset asset,
            MarketDataProviderType providerType,
            String refreshSource
    ) {
        try {
            return refreshExternalPrice(asset, providerType, refreshSource);
        } catch (RestClientException exception) {
            log.warn(
                    "{} refresh request failed for {}. Falling back to DEMO refresh. Reason: {}",
                    providerType,
                    asset.getTicker(),
                    exception.getMessage()
            );

            return refreshDemoPrice(asset);
        } catch (RuntimeException exception) {
            log.warn(
                    "{} refresh failed for {}. Falling back to DEMO refresh. Reason: {}",
                    providerType,
                    asset.getTicker(),
                    exception.getMessage()
            );

            return refreshDemoPrice(asset);
        }
    }

    private MarketPriceResponse refreshExternalPrice(
            Asset asset,
            MarketDataProviderType providerType,
            String refreshSource
    ) {
        MarketDataProvider provider = marketDataProviderRegistry.getProvider(providerType);

        MarketPriceResponse livePrice = provider.getLatestMarketPrice(asset);

        MarketPrice refreshedMarketPrice = MarketPrice.builder()
                .asset(asset)
                .price(livePrice.price())
                .volume(livePrice.volume())
                .source(refreshSource)
                .timestamp(livePrice.timestamp())
                .build();

        MarketPrice savedPrice = marketPriceRepository.save(refreshedMarketPrice);

        return marketDataMapper.toMarketPriceResponse(savedPrice);
    }

    private MarketPriceResponse refreshDemoPrice(Asset asset) {
        MarketPrice latestPrice = marketPriceRepository.findTopByAssetOrderByTimestampDesc(asset)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Market price not found for asset: " + asset.getTicker()
                ));

        BigDecimal refreshedPrice = applyRandomPercentChange(
                latestPrice.getPrice(),
                -0.015,
                0.015
        );

        BigDecimal refreshedVolume = applyRandomPercentChange(
                latestPrice.getVolume(),
                -0.10,
                0.10
        );

        MarketPrice refreshedMarketPrice = MarketPrice.builder()
                .asset(asset)
                .price(refreshedPrice)
                .volume(refreshedVolume)
                .source("DEMO_REFRESH")
                .timestamp(Instant.now())
                .build();

        MarketPrice savedPrice = marketPriceRepository.save(refreshedMarketPrice);

        return marketDataMapper.toMarketPriceResponse(savedPrice);
    }

    private BigDecimal applyRandomPercentChange(
            BigDecimal value,
            double minPercent,
            double maxPercent
    ) {
        double randomPercent = ThreadLocalRandom.current().nextDouble(minPercent, maxPercent);

        return value
                .multiply(BigDecimal.ONE.add(BigDecimal.valueOf(randomPercent)))
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    private void validateAssetType(
            Asset asset,
            AssetType expectedType,
            String message
    ) {
        if (asset.getAssetType() != expectedType) {
            throw new BadRequestException(message);
        }
    }
}
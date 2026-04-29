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

        if (marketDataProperties.provider() == MarketDataProviderType.HYBRID
                && asset.getAssetType() == AssetType.CRYPTO) {
            return refreshCryptoPriceWithFallback(asset);
        }

        if (marketDataProperties.provider() != MarketDataProviderType.DEMO
                && marketDataProperties.provider() != MarketDataProviderType.HYBRID) {
            throw new BadRequestException(
                    "Manual refresh is currently available only for DEMO or HYBRID market data provider"
            );
        }

        return refreshDemoPrice(asset);
    }

    private MarketPriceResponse refreshCryptoPriceWithFallback(Asset asset) {
        try {
            return refreshCryptoPriceFromBinance(asset);
        } catch (RestClientException exception) {
            log.warn(
                    "Binance refresh request failed for {}. Falling back to DEMO refresh. Reason: {}",
                    asset.getTicker(),
                    exception.getMessage()
            );

            return refreshDemoPrice(asset);
        } catch (RuntimeException exception) {
            log.warn(
                    "Binance refresh failed for {}. Falling back to DEMO refresh. Reason: {}",
                    asset.getTicker(),
                    exception.getMessage()
            );

            return refreshDemoPrice(asset);
        }
    }

    private MarketPriceResponse refreshCryptoPriceFromBinance(Asset asset) {
        MarketDataProvider binanceProvider = marketDataProviderRegistry.getProvider(
                MarketDataProviderType.BINANCE
        );

        MarketPriceResponse livePrice = binanceProvider.getLatestMarketPrice(asset);

        MarketPrice refreshedMarketPrice = MarketPrice.builder()
                .asset(asset)
                .price(livePrice.price())
                .volume(livePrice.volume())
                .source("BINANCE_REFRESH")
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
}
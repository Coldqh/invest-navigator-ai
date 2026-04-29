package com.investnavigator.backend.marketdata.service;

import com.investnavigator.backend.asset.model.Asset;
import com.investnavigator.backend.asset.repository.AssetRepository;
import com.investnavigator.backend.common.error.ResourceNotFoundException;
import com.investnavigator.backend.marketdata.config.MarketDataProperties;
import com.investnavigator.backend.marketdata.dto.CandleResponse;
import com.investnavigator.backend.marketdata.dto.MarketPriceResponse;
import com.investnavigator.backend.marketdata.model.Timeframe;
import com.investnavigator.backend.marketdata.provider.MarketDataProvider;
import com.investnavigator.backend.marketdata.provider.MarketDataProviderRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketDataService {

    private final AssetRepository assetRepository;
    private final MarketDataProperties marketDataProperties;
    private final MarketDataProviderRegistry marketDataProviderRegistry;

    public MarketPriceResponse getLatestMarketPrice(String ticker) {
        Asset asset = findAssetByTicker(ticker);
        MarketDataProvider provider = getActiveProvider();

        return provider.getLatestMarketPrice(asset);
    }

    public List<CandleResponse> getCandles(String ticker, Timeframe timeframe) {
        Asset asset = findAssetByTicker(ticker);
        MarketDataProvider provider = getActiveProvider();

        return provider.getCandles(asset, timeframe);
    }

    private MarketDataProvider getActiveProvider() {
        return marketDataProviderRegistry.getProvider(marketDataProperties.provider());
    }

    private Asset findAssetByTicker(String ticker) {
        return assetRepository.findByTickerIgnoreCase(ticker)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + ticker));
    }
}
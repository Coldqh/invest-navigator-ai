package com.investnavigator.backend.marketdata.provider;

import com.investnavigator.backend.asset.model.Asset;
import com.investnavigator.backend.asset.model.AssetType;
import com.investnavigator.backend.marketdata.dto.CandleResponse;
import com.investnavigator.backend.marketdata.dto.MarketPriceResponse;
import com.investnavigator.backend.marketdata.model.Timeframe;
import com.investnavigator.backend.marketdata.provider.binance.BinanceMarketDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HybridMarketDataProvider implements MarketDataProvider {

    private final DemoMarketDataProvider demoMarketDataProvider;
    private final BinanceMarketDataProvider binanceMarketDataProvider;

    @Override
    public MarketDataProviderType getType() {
        return MarketDataProviderType.HYBRID;
    }

    @Override
    public MarketPriceResponse getLatestMarketPrice(Asset asset) {
        if (asset.getAssetType() == AssetType.CRYPTO) {
            return binanceMarketDataProvider.getLatestMarketPrice(asset);
        }

        return demoMarketDataProvider.getLatestMarketPrice(asset);
    }

    @Override
    public List<CandleResponse> getCandles(Asset asset, Timeframe timeframe) {
        if (asset.getAssetType() == AssetType.CRYPTO) {
            return binanceMarketDataProvider.getCandles(asset, timeframe);
        }

        return demoMarketDataProvider.getCandles(asset, timeframe);
    }
}
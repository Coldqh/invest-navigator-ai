package com.investnavigator.backend.marketdata.provider;

import com.investnavigator.backend.asset.model.Asset;
import com.investnavigator.backend.asset.model.AssetType;
import com.investnavigator.backend.marketdata.dto.CandleResponse;
import com.investnavigator.backend.marketdata.dto.MarketPriceResponse;
import com.investnavigator.backend.marketdata.model.Timeframe;
import com.investnavigator.backend.marketdata.provider.binance.BinanceMarketDataProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HybridMarketDataProvider implements MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(HybridMarketDataProvider.class);

    private final DemoMarketDataProvider demoMarketDataProvider;
    private final BinanceMarketDataProvider binanceMarketDataProvider;

    @Override
    public MarketDataProviderType getType() {
        return MarketDataProviderType.HYBRID;
    }

    @Override
    public MarketPriceResponse getLatestMarketPrice(Asset asset) {
        if (asset.getAssetType() != AssetType.CRYPTO) {
            return demoMarketDataProvider.getLatestMarketPrice(asset);
        }

        try {
            return binanceMarketDataProvider.getLatestMarketPrice(asset);
        } catch (RestClientException exception) {
            log.warn(
                    "Binance price request failed for {}. Falling back to DEMO provider. Reason: {}",
                    asset.getTicker(),
                    exception.getMessage()
            );

            return demoMarketDataProvider.getLatestMarketPrice(asset);
        } catch (RuntimeException exception) {
            log.warn(
                    "Binance provider failed for {}. Falling back to DEMO provider. Reason: {}",
                    asset.getTicker(),
                    exception.getMessage()
            );

            return demoMarketDataProvider.getLatestMarketPrice(asset);
        }
    }

    @Override
    public List<CandleResponse> getCandles(Asset asset, Timeframe timeframe) {
        if (asset.getAssetType() != AssetType.CRYPTO) {
            return demoMarketDataProvider.getCandles(asset, timeframe);
        }

        try {
            return binanceMarketDataProvider.getCandles(asset, timeframe);
        } catch (RestClientException exception) {
            log.warn(
                    "Binance candles request failed for {}. Falling back to DEMO provider. Reason: {}",
                    asset.getTicker(),
                    exception.getMessage()
            );

            return demoMarketDataProvider.getCandles(asset, timeframe);
        } catch (RuntimeException exception) {
            log.warn(
                    "Binance provider failed for {} candles. Falling back to DEMO provider. Reason: {}",
                    asset.getTicker(),
                    exception.getMessage()
            );

            return demoMarketDataProvider.getCandles(asset, timeframe);
        }
    }
}
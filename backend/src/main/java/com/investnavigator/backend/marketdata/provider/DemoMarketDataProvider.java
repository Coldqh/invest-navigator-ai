package com.investnavigator.backend.marketdata.provider;

import com.investnavigator.backend.asset.model.Asset;
import com.investnavigator.backend.common.error.ResourceNotFoundException;
import com.investnavigator.backend.marketdata.dto.CandleResponse;
import com.investnavigator.backend.marketdata.dto.MarketPriceResponse;
import com.investnavigator.backend.marketdata.mapper.MarketDataMapper;
import com.investnavigator.backend.marketdata.model.Timeframe;
import com.investnavigator.backend.marketdata.repository.CandleRepository;
import com.investnavigator.backend.marketdata.repository.MarketPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DemoMarketDataProvider implements MarketDataProvider {

    private final MarketPriceRepository marketPriceRepository;
    private final CandleRepository candleRepository;
    private final MarketDataMapper marketDataMapper;

    @Override
    public MarketDataProviderType getType() {
        return MarketDataProviderType.DEMO;
    }

    @Override
    public MarketPriceResponse getLatestMarketPrice(Asset asset) {
        return marketPriceRepository.findTopByAssetOrderByTimestampDesc(asset)
                .map(marketDataMapper::toMarketPriceResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Market price not found for asset: " + asset.getTicker()
                ));
    }

    @Override
    public List<CandleResponse> getCandles(Asset asset, Timeframe timeframe) {
        return candleRepository.findByAssetAndTimeframeOrderByTimestampAsc(asset, timeframe)
                .stream()
                .map(marketDataMapper::toCandleResponse)
                .toList();
    }
}
package com.investnavigator.backend.marketdata.service;

import com.investnavigator.backend.asset.model.Asset;
import com.investnavigator.backend.asset.repository.AssetRepository;
import com.investnavigator.backend.marketdata.dto.CandleResponse;
import com.investnavigator.backend.marketdata.dto.MarketPriceResponse;
import com.investnavigator.backend.marketdata.mapper.MarketDataMapper;
import com.investnavigator.backend.marketdata.model.Timeframe;
import com.investnavigator.backend.marketdata.repository.CandleRepository;
import com.investnavigator.backend.marketdata.repository.MarketPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.investnavigator.backend.common.error.ResourceNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketDataService {

    private final AssetRepository assetRepository;
    private final MarketPriceRepository marketPriceRepository;
    private final CandleRepository candleRepository;
    private final MarketDataMapper marketDataMapper;

    public MarketPriceResponse getLatestMarketPrice(String ticker) {
        Asset asset = findAssetByTicker(ticker);

        return marketPriceRepository.findTopByAssetOrderByTimestampDesc(asset)
                .map(marketDataMapper::toMarketPriceResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Market price not found for asset: " + ticker));
    }

    public List<CandleResponse> getCandles(String ticker, Timeframe timeframe) {
        Asset asset = findAssetByTicker(ticker);

        return candleRepository.findByAssetAndTimeframeOrderByTimestampAsc(asset, timeframe)
                .stream()
                .map(marketDataMapper::toCandleResponse)
                .toList();
    }

    private Asset findAssetByTicker(String ticker) {
        return assetRepository.findByTickerIgnoreCase(ticker)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + ticker));
    }
}
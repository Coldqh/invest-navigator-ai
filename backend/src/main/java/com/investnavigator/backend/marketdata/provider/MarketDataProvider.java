package com.investnavigator.backend.marketdata.provider;

import com.investnavigator.backend.asset.model.Asset;
import com.investnavigator.backend.marketdata.dto.CandleResponse;
import com.investnavigator.backend.marketdata.dto.MarketPriceResponse;
import com.investnavigator.backend.marketdata.model.Timeframe;

import java.util.List;

public interface MarketDataProvider {

    MarketDataProviderType getType();

    MarketPriceResponse getLatestMarketPrice(Asset asset);

    List<CandleResponse> getCandles(Asset asset, Timeframe timeframe);
}
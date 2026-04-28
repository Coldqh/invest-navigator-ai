package com.investnavigator.backend.marketdata.mapper;

import com.investnavigator.backend.marketdata.dto.CandleResponse;
import com.investnavigator.backend.marketdata.dto.MarketPriceResponse;
import com.investnavigator.backend.marketdata.model.Candle;
import com.investnavigator.backend.marketdata.model.MarketPrice;
import org.springframework.stereotype.Component;

@Component
public class MarketDataMapper {

    public MarketPriceResponse toMarketPriceResponse(MarketPrice marketPrice) {
        return new MarketPriceResponse(
                marketPrice.getAsset().getId(),
                marketPrice.getAsset().getTicker(),
                marketPrice.getAsset().getName(),
                marketPrice.getPrice(),
                marketPrice.getVolume(),
                marketPrice.getSource(),
                marketPrice.getTimestamp()
        );
    }

    public CandleResponse toCandleResponse(Candle candle) {
        return new CandleResponse(
                candle.getTimeframe(),
                candle.getOpen(),
                candle.getHigh(),
                candle.getLow(),
                candle.getClose(),
                candle.getVolume(),
                candle.getSource(),
                candle.getTimestamp()
        );
    }
}
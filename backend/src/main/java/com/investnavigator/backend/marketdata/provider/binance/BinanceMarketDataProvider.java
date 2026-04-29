package com.investnavigator.backend.marketdata.provider.binance;

import com.investnavigator.backend.asset.model.Asset;
import com.investnavigator.backend.asset.model.AssetType;
import com.investnavigator.backend.common.error.BadRequestException;
import com.investnavigator.backend.common.error.ResourceNotFoundException;
import com.investnavigator.backend.marketdata.dto.CandleResponse;
import com.investnavigator.backend.marketdata.dto.MarketPriceResponse;
import com.investnavigator.backend.marketdata.model.Timeframe;
import com.investnavigator.backend.marketdata.provider.MarketDataProvider;
import com.investnavigator.backend.marketdata.provider.MarketDataProviderType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class BinanceMarketDataProvider implements MarketDataProvider {

    private final RestClient binanceRestClient;

    public BinanceMarketDataProvider(
            @Qualifier("binanceRestClient") RestClient binanceRestClient
    ) {
        this.binanceRestClient = binanceRestClient;
    }

    @Override
    public MarketDataProviderType getType() {
        return MarketDataProviderType.BINANCE;
    }

    @Override
    public MarketPriceResponse getLatestMarketPrice(Asset asset) {
        validateCryptoAsset(asset);

        BinanceTicker24hResponse response = binanceRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v3/ticker/24hr")
                        .queryParam("symbol", asset.getTicker().toUpperCase())
                        .build()
                )
                .retrieve()
                .body(BinanceTicker24hResponse.class);

        if (response == null || response.lastPrice() == null) {
            throw new ResourceNotFoundException(
                    "Binance market price not found for asset: " + asset.getTicker()
            );
        }

        Instant timestamp = response.closeTime() == null
                ? Instant.now()
                : Instant.ofEpochMilli(response.closeTime());

        return new MarketPriceResponse(
                asset.getId(),
                asset.getTicker(),
                asset.getName(),
                response.lastPrice(),
                response.volume() == null ? BigDecimal.ZERO : response.volume(),
                "BINANCE",
                timestamp
        );
    }

    @Override
    public List<CandleResponse> getCandles(Asset asset, Timeframe timeframe) {
        validateCryptoAsset(asset);

        List<List<Object>> response = binanceRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v3/klines")
                        .queryParam("symbol", asset.getTicker().toUpperCase())
                        .queryParam("interval", toBinanceInterval(timeframe))
                        .queryParam("limit", 30)
                        .build()
                )
                .retrieve()
                .body(List.class);

        if (response == null || response.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Binance candles not found for asset: " + asset.getTicker()
            );
        }

        return response.stream()
                .map(row -> toCandleResponse(row, timeframe))
                .toList();
    }

    private CandleResponse toCandleResponse(List<Object> row, Timeframe timeframe) {
        long openTime = Long.parseLong(row.get(0).toString());

        return new CandleResponse(
                timeframe,
                toBigDecimal(row.get(1)),
                toBigDecimal(row.get(2)),
                toBigDecimal(row.get(3)),
                toBigDecimal(row.get(4)),
                toBigDecimal(row.get(5)),
                "BINANCE",
                Instant.ofEpochMilli(openTime)
        );
    }

    private BigDecimal toBigDecimal(Object value) {
        return new BigDecimal(value.toString());
    }

    private String toBinanceInterval(Timeframe timeframe) {
        return switch (timeframe) {
            case ONE_MINUTE -> "1m";
            case FIVE_MINUTES -> "5m";
            case FIFTEEN_MINUTES -> "15m";
            case ONE_HOUR -> "1h";
            case ONE_DAY -> "1d";
        };
    }

    private void validateCryptoAsset(Asset asset) {
        if (asset.getAssetType() != AssetType.CRYPTO) {
            throw new BadRequestException(
                    "Binance provider supports only crypto assets"
            );
        }
    }
}
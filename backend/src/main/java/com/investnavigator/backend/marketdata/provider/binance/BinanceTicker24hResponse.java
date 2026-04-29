package com.investnavigator.backend.marketdata.provider.binance;

import java.math.BigDecimal;

public record BinanceTicker24hResponse(
        String symbol,
        BigDecimal lastPrice,
        BigDecimal volume,
        Long closeTime
) {
}
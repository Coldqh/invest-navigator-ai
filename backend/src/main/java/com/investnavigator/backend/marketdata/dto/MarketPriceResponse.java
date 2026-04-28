package com.investnavigator.backend.marketdata.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MarketPriceResponse(
        UUID assetId,
        String ticker,
        String name,
        BigDecimal price,
        BigDecimal volume,
        String source,
        Instant timestamp
) {
}
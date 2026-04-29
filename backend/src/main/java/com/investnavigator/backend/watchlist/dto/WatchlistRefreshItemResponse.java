package com.investnavigator.backend.watchlist.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record WatchlistRefreshItemResponse(
        String ticker,
        String name,
        boolean refreshed,
        BigDecimal price,
        BigDecimal volume,
        String source,
        Instant timestamp,
        String errorMessage
) {
}
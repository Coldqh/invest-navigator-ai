package com.investnavigator.backend.watchlist.dto;

import java.time.Instant;
import java.util.List;

public record WatchlistRefreshResponse(
        int totalItems,
        int refreshedItems,
        int failedItems,
        List<WatchlistRefreshItemResponse> items,
        Instant refreshedAt
) {
}
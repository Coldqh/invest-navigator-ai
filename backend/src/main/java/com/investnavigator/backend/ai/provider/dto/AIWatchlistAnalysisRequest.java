package com.investnavigator.backend.ai.provider.dto;

import java.time.Instant;
import java.util.List;

public record AIWatchlistAnalysisRequest(
        List<AIWatchlistItemSnapshot> items,
        Instant generatedAt
) {
}
package com.investnavigator.backend.ai.provider.dto;

import java.time.Instant;
import java.util.List;

public record AICompareAnalysisRequest(
        List<AICompareAssetSnapshot> assets,
        Instant generatedAt
) {
}
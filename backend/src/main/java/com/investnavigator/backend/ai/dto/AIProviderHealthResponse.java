package com.investnavigator.backend.ai.dto;

import com.investnavigator.backend.ai.provider.AIProviderType;

import java.time.Instant;
import java.util.List;

public record AIProviderHealthResponse(
        AIProviderType activeProvider,
        AIProviderHealthStatus status,
        List<AIProviderHealthItemResponse> providers,
        Instant checkedAt
) {
}
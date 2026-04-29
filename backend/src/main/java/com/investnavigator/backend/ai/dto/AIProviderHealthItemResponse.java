package com.investnavigator.backend.ai.dto;

import com.investnavigator.backend.ai.provider.AIProviderType;

import java.time.Instant;

public record AIProviderHealthItemResponse(
        AIProviderType type,
        AIProviderHealthStatus status,
        String message,
        Instant checkedAt
) {
}
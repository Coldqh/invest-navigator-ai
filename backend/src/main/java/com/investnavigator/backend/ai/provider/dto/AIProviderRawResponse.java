package com.investnavigator.backend.ai.provider.dto;

import com.investnavigator.backend.ai.provider.AIProviderType;

import java.time.Instant;

public record AIProviderRawResponse(
        AIProviderType providerType,
        String model,
        String rawText,
        Instant createdAt
) {
}
package com.investnavigator.backend.ai.dto;

import com.investnavigator.backend.ai.provider.AIProviderType;

public record AIProviderConfigurationResponse(
        AIProviderType activeProvider,
        ExternalProviderConfiguration yandexGpt,
        ExternalProviderConfiguration gigaChat,
        ExternalProviderConfiguration openAi
) {
    public record ExternalProviderConfiguration(
            boolean enabled,
            String baseUrl,
            boolean apiKeyConfigured,
            boolean modelConfigured,
            String model,
            int timeoutSeconds
    ) {
    }
}
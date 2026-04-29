package com.investnavigator.backend.ai.config;

import com.investnavigator.backend.ai.provider.AIProviderType;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public record AIProperties(
        AIProviderType provider
) {
    public AIProperties {
        if (provider == null) {
            provider = AIProviderType.MOCK;
        }
    }
}
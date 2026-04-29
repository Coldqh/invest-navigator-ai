package com.investnavigator.backend.ai.controller;

import com.investnavigator.backend.ai.config.AIProperties;
import com.investnavigator.backend.ai.dto.AIProviderConfigurationResponse;
import com.investnavigator.backend.ai.dto.AIProviderHealthResponse;
import com.investnavigator.backend.ai.provider.AIProvider;
import com.investnavigator.backend.ai.provider.AIProviderRegistry;
import com.investnavigator.backend.ai.service.AIProviderHealthService;
import com.investnavigator.backend.ai.service.YandexGptConnectionTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/provider")
@RequiredArgsConstructor
public class AIProviderController {

    private final AIProperties aiProperties;
    private final AIProviderRegistry aiProviderRegistry;
    private final AIProviderHealthService aiProviderHealthService;
    private final YandexGptConnectionTestService yandexGptConnectionTestService;

    @GetMapping
    public Map<String, Object> getActiveProvider() {
        AIProvider provider = aiProviderRegistry.getProvider(aiProperties.provider());
        AIProviderHealthResponse health = aiProviderHealthService.getHealth();

        return Map.of(
                "activeProvider", provider.getType(),
                "status", health.status()
        );
    }

    @GetMapping("/health")
    public AIProviderHealthResponse getProviderHealth() {
        return aiProviderHealthService.getHealth();
    }

    @GetMapping("/configuration")
    public AIProviderConfigurationResponse getProviderConfiguration() {
        return new AIProviderConfigurationResponse(
                aiProperties.provider(),
                toExternalProviderConfiguration(aiProperties.yandexGpt()),
                toExternalProviderConfiguration(aiProperties.gigaChat()),
                toExternalProviderConfiguration(aiProperties.openAi())
        );
    }

    @GetMapping("/yandex/test")
    public Map<String, Object> testYandexGptConnection() {
        return yandexGptConnectionTestService.testConnection();
    }

    private AIProviderConfigurationResponse.ExternalProviderConfiguration toExternalProviderConfiguration(
            AIProperties.ExternalProvider properties
    ) {
        return new AIProviderConfigurationResponse.ExternalProviderConfiguration(
                properties.enabled(),
                properties.baseUrl(),
                properties.hasApiKey(),
                properties.hasModel(),
                properties.hasFolderId(),
                properties.model(),
                properties.timeoutSeconds()
        );
    }
}
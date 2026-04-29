package com.investnavigator.backend.ai.service;

import com.investnavigator.backend.ai.config.AIProperties;
import com.investnavigator.backend.ai.dto.AIProviderHealthItemResponse;
import com.investnavigator.backend.ai.dto.AIProviderHealthResponse;
import com.investnavigator.backend.ai.dto.AIProviderHealthStatus;
import com.investnavigator.backend.ai.provider.AIProviderRegistry;
import com.investnavigator.backend.ai.provider.AIProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AIProviderHealthService {

    private final AIProperties aiProperties;
    private final AIProviderRegistry aiProviderRegistry;

    public AIProviderHealthResponse getHealth() {
        Instant checkedAt = Instant.now();

        List<AIProviderHealthItemResponse> providers = List.of(
                checkMockProvider(checkedAt),
                checkExternalProvider(
                        AIProviderType.YANDEX_GPT,
                        "YandexGPT",
                        aiProperties.yandexGpt(),
                        checkedAt
                ),
                checkExternalProvider(
                        AIProviderType.GIGA_CHAT,
                        "GigaChat",
                        aiProperties.gigaChat(),
                        checkedAt
                ),
                checkExternalProvider(
                        AIProviderType.OPENAI,
                        "OpenAI",
                        aiProperties.openAi(),
                        checkedAt
                )
        );

        AIProviderHealthStatus overallStatus = calculateOverallStatus(providers);

        return new AIProviderHealthResponse(
                aiProperties.provider(),
                overallStatus,
                providers,
                checkedAt
        );
    }

    private AIProviderHealthItemResponse checkMockProvider(Instant checkedAt) {
        try {
            aiProviderRegistry.getProvider(AIProviderType.MOCK);

            return new AIProviderHealthItemResponse(
                    AIProviderType.MOCK,
                    AIProviderHealthStatus.AVAILABLE,
                    "Mock AI provider is available",
                    checkedAt
            );
        } catch (RuntimeException exception) {
            return new AIProviderHealthItemResponse(
                    AIProviderType.MOCK,
                    AIProviderHealthStatus.UNAVAILABLE,
                    "Mock AI provider is unavailable: " + exception.getMessage(),
                    checkedAt
            );
        }
    }

    private AIProviderHealthItemResponse checkExternalProvider(
            AIProviderType type,
            String displayName,
            AIProperties.ExternalProvider properties,
            Instant checkedAt
    ) {
        if (!properties.enabled()) {
            return new AIProviderHealthItemResponse(
                    type,
                    AIProviderHealthStatus.NOT_CONFIGURED,
                    displayName + " provider is disabled in application config",
                    checkedAt
            );
        }

        if (!properties.hasApiKey()) {
            return new AIProviderHealthItemResponse(
                    type,
                    AIProviderHealthStatus.NOT_CONFIGURED,
                    displayName + " provider is enabled, but api-key is empty",
                    checkedAt
            );
        }

        if (!properties.hasModel()) {
            return new AIProviderHealthItemResponse(
                    type,
                    AIProviderHealthStatus.NOT_CONFIGURED,
                    displayName + " provider is enabled, but model is empty",
                    checkedAt
            );
        }

        try {
            aiProviderRegistry.getProvider(type);

            return new AIProviderHealthItemResponse(
                    type,
                    AIProviderHealthStatus.AVAILABLE,
                    displayName + " provider is configured and available",
                    checkedAt
            );
        } catch (RuntimeException exception) {
            return new AIProviderHealthItemResponse(
                    type,
                    AIProviderHealthStatus.DEGRADED,
                    displayName + " provider config is ready, but implementation is missing: " + exception.getMessage(),
                    checkedAt
            );
        }
    }

    private AIProviderHealthStatus calculateOverallStatus(
            List<AIProviderHealthItemResponse> providers
    ) {
        return providers.stream()
                .filter(provider -> provider.type() == aiProperties.provider())
                .map(AIProviderHealthItemResponse::status)
                .findFirst()
                .orElse(AIProviderHealthStatus.NOT_CONFIGURED);
    }
}
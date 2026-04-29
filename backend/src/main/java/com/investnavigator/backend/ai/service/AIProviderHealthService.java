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
                notConfigured(AIProviderType.YANDEX_GPT, "YandexGPT provider is not implemented yet", checkedAt),
                notConfigured(AIProviderType.GIGA_CHAT, "GigaChat provider is not implemented yet", checkedAt),
                notConfigured(AIProviderType.OPENAI, "OpenAI provider is not implemented yet", checkedAt)
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

    private AIProviderHealthItemResponse notConfigured(
            AIProviderType type,
            String message,
            Instant checkedAt
    ) {
        return new AIProviderHealthItemResponse(
                type,
                AIProviderHealthStatus.NOT_CONFIGURED,
                message,
                checkedAt
        );
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
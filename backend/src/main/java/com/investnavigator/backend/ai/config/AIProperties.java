package com.investnavigator.backend.ai.config;

import com.investnavigator.backend.ai.provider.AIProviderType;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public record AIProperties(
        AIProviderType provider,
        ExternalProvider yandexGpt,
        ExternalProvider gigaChat,
        ExternalProvider openAi
) {
    public AIProperties {
        if (provider == null) {
            provider = AIProviderType.MOCK;
        }

        if (yandexGpt == null) {
            yandexGpt = new ExternalProvider(
                    false,
                    "https://llm.api.cloud.yandex.net/v1",
                    "",
                    "",
                    "",
                    30
            );
        }

        if (gigaChat == null) {
            gigaChat = new ExternalProvider(
                    false,
                    "https://gigachat.devices.sberbank.ru/api/v1",
                    "",
                    "",
                    "",
                    30
            );
        }

        if (openAi == null) {
            openAi = new ExternalProvider(
                    false,
                    "https://api.openai.com/v1",
                    "",
                    "",
                    "",
                    30
            );
        }
    }

    public record ExternalProvider(
            boolean enabled,
            String baseUrl,
            String apiKey,
            String model,
            String folderId,
            int timeoutSeconds
    ) {
        public ExternalProvider {
            if (baseUrl == null || baseUrl.isBlank()) {
                baseUrl = "";
            }

            if (apiKey == null) {
                apiKey = "";
            }

            if (model == null) {
                model = "";
            }

            if (folderId == null) {
                folderId = "";
            }

            if (timeoutSeconds <= 0) {
                timeoutSeconds = 30;
            }
        }

        public boolean hasApiKey() {
            return apiKey != null && !apiKey.isBlank();
        }

        public boolean hasModel() {
            return model != null && !model.isBlank();
        }

        public boolean hasFolderId() {
            return folderId != null && !folderId.isBlank();
        }

        public boolean isReady() {
            return enabled && hasApiKey() && hasModel();
        }
    }
}
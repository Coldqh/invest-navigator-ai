package com.investnavigator.backend.ai.provider.yandex;

import com.investnavigator.backend.ai.config.AIProperties;
import com.investnavigator.backend.ai.provider.AIProvider;
import com.investnavigator.backend.ai.provider.AIProviderType;
import com.investnavigator.backend.ai.provider.dto.AIAnalysisRequest;
import com.investnavigator.backend.ai.provider.dto.AIAnalysisResult;
import com.investnavigator.backend.ai.provider.dto.AIProviderPrompt;
import com.investnavigator.backend.ai.provider.dto.AIProviderRawResponse;
import com.investnavigator.backend.ai.provider.parser.AIProviderJsonParser;
import com.investnavigator.backend.ai.provider.prompt.AIProviderPromptBuilder;
import com.investnavigator.backend.common.error.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class YandexGptAIProvider implements AIProvider {

    private static final String AUTHORIZATION_HEADER_PREFIX = "Api-Key ";
    private static final BigDecimal DEFAULT_TEMPERATURE = BigDecimal.valueOf(0.2);
    private static final int DEFAULT_MAX_TOKENS = 1200;

    private final AIProperties aiProperties;
    private final AIProviderPromptBuilder promptBuilder;
    private final AIProviderJsonParser jsonParser;

    @Qualifier("yandexGptRestClient")
    private final RestClient yandexGptRestClient;

    @Override
    public AIProviderType getType() {
        return AIProviderType.YANDEX_GPT;
    }

    @Override
    public AIAnalysisResult analyze(AIAnalysisRequest request) {
        AIProperties.ExternalProvider providerProperties = aiProperties.yandexGpt();

        validateConfiguration(providerProperties);

        AIProviderPrompt prompt = promptBuilder.buildPrompt(request);

        YandexGptChatCompletionRequest chatCompletionRequest = new YandexGptChatCompletionRequest(
                providerProperties.model(),
                List.of(
                        new YandexGptChatMessage("system", prompt.systemPrompt()),
                        new YandexGptChatMessage("user", prompt.userPrompt())
                ),
                DEFAULT_TEMPERATURE,
                DEFAULT_MAX_TOKENS
        );

        YandexGptChatCompletionResponse response = yandexGptRestClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(
                        "Authorization",
                        AUTHORIZATION_HEADER_PREFIX + providerProperties.apiKey()
                )
                .body(chatCompletionRequest)
                .retrieve()
                .body(YandexGptChatCompletionResponse.class);

        String rawText = response == null ? "" : response.firstMessageContent();

        AIProviderRawResponse rawResponse = new AIProviderRawResponse(
                getType(),
                providerProperties.model(),
                rawText,
                Instant.now()
        );

        return jsonParser.parse(rawResponse.rawText(), request.analytics());
    }

    private void validateConfiguration(AIProperties.ExternalProvider providerProperties) {
        if (!providerProperties.enabled()) {
            throw new BadRequestException("YandexGPT provider is disabled in application config");
        }

        if (!providerProperties.hasApiKey()) {
            throw new BadRequestException("YandexGPT api-key is not configured");
        }

        if (!providerProperties.hasModel()) {
            throw new BadRequestException("YandexGPT model is not configured");
        }
    }
}
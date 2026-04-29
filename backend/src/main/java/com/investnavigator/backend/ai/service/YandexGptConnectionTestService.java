package com.investnavigator.backend.ai.service;

import com.investnavigator.backend.ai.config.AIProperties;
import com.investnavigator.backend.ai.provider.yandex.YandexGptChatCompletionRequest;
import com.investnavigator.backend.ai.provider.yandex.YandexGptChatCompletionResponse;
import com.investnavigator.backend.ai.provider.yandex.YandexGptChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class YandexGptConnectionTestService {

    private static final String AUTHORIZATION_HEADER_PREFIX = "Bearer ";
    private static final String OPENAI_PROJECT_HEADER = "OpenAI-Project";

    private final AIProperties aiProperties;

    @Qualifier("yandexGptRestClient")
    private final RestClient yandexGptRestClient;

    public Map<String, Object> testConnection() {
        AIProperties.ExternalProvider properties = aiProperties.yandexGpt();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("testedAt", Instant.now());
        result.put("enabled", properties.enabled());
        result.put("baseUrl", properties.baseUrl());
        result.put("apiKeyConfigured", properties.hasApiKey());
        result.put("folderIdConfigured", properties.hasFolderId());
        result.put("folderId", maskFolderId(properties.folderId()));
        result.put("modelConfigured", properties.hasModel());
        result.put("model", properties.model());

        if (!properties.enabled()) {
            result.put("ok", false);
            result.put("reason", "YandexGPT is disabled in config");
            return result;
        }

        if (!properties.hasApiKey()) {
            result.put("ok", false);
            result.put("reason", "YandexGPT api-key is empty");
            return result;
        }

        if (!properties.hasFolderId()) {
            result.put("ok", false);
            result.put("reason", "YandexGPT folder-id is empty");
            return result;
        }

        if (!properties.hasModel()) {
            result.put("ok", false);
            result.put("reason", "YandexGPT model is empty");
            return result;
        }

        try {
            YandexGptChatCompletionRequest request = new YandexGptChatCompletionRequest(
                    properties.model(),
                    List.of(
                            new YandexGptChatMessage(
                                    "system",
                                    "Return only a short plain text answer."
                            ),
                            new YandexGptChatMessage(
                                    "user",
                                    "Answer with the word OK."
                            )
                    ),
                    BigDecimal.ZERO,
                    64,
                    false,
                    null
            );

            YandexGptChatCompletionResponse response = yandexGptRestClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", AUTHORIZATION_HEADER_PREFIX + properties.apiKey())
                    .header(OPENAI_PROJECT_HEADER, properties.folderId())
                    .body(request)
                    .retrieve()
                    .body(YandexGptChatCompletionResponse.class);

            String content = response == null ? "" : response.firstMessageContent();

            result.put("ok", true);
            result.put("httpStatus", "200 OK");
            result.put("responsePreview", content);
            result.put("message", "YandexGPT direct test request completed successfully");

            return result;
        } catch (RestClientResponseException exception) {
            result.put("ok", false);
            result.put("httpStatus", exception.getStatusCode().toString());
            result.put("responseBody", exception.getResponseBodyAsString());
            result.put("message", "YandexGPT direct test request failed");

            return result;
        } catch (RuntimeException exception) {
            result.put("ok", false);
            result.put("exception", exception.getClass().getSimpleName());
            result.put("message", exception.getMessage());

            return result;
        }
    }

    private String maskFolderId(String folderId) {
        if (folderId == null || folderId.isBlank()) {
            return "";
        }

        if (folderId.length() <= 8) {
            return folderId;
        }

        return folderId.substring(0, 4) + "..." + folderId.substring(folderId.length() - 4);
    }
}
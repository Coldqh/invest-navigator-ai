package com.investnavigator.backend.ai.provider.yandex;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record YandexGptChatCompletionRequest(
        @JsonProperty("model")
        String model,

        @JsonProperty("messages")
        List<YandexGptChatMessage> messages,

        @JsonProperty("temperature")
        BigDecimal temperature,

        @JsonProperty("max_tokens")
        int maxTokens,

        @JsonProperty("stream")
        boolean stream,

        @JsonProperty("response_format")
        Map<String, Object> responseFormat
) {
}
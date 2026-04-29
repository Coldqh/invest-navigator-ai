package com.investnavigator.backend.ai.provider.yandex;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

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
        boolean stream
) {
}
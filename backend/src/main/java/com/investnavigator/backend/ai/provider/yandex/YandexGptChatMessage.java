package com.investnavigator.backend.ai.provider.yandex;

import com.fasterxml.jackson.annotation.JsonProperty;

public record YandexGptChatMessage(
        @JsonProperty("role")
        String role,

        @JsonProperty("content")
        String content
) {
}
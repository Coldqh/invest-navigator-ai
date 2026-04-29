package com.investnavigator.backend.ai.provider.yandex;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YandexGptChatCompletionResponse(
        @JsonProperty("id")
        String id,

        @JsonProperty("choices")
        List<Choice> choices
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(
            @JsonProperty("index")
            Integer index,

            @JsonProperty("message")
            Message message,

            @JsonProperty("finish_reason")
            String finishReason
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(
            @JsonProperty("role")
            String role,

            @JsonProperty("content")
            String content
    ) {
    }

    public String firstMessageContent() {
        if (choices == null || choices.isEmpty()) {
            return "";
        }

        Choice firstChoice = choices.get(0);

        if (firstChoice == null || firstChoice.message() == null) {
            return "";
        }

        return firstChoice.message().content() == null
                ? ""
                : firstChoice.message().content();
    }
}
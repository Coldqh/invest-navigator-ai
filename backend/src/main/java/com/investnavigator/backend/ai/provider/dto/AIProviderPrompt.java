package com.investnavigator.backend.ai.provider.dto;

public record AIProviderPrompt(
        String systemPrompt,
        String userPrompt
) {
}
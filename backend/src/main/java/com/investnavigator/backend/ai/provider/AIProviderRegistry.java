package com.investnavigator.backend.ai.provider;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AIProviderRegistry {

    private final List<AIProvider> providers;

    public AIProvider getProvider(AIProviderType type) {
        return providers.stream()
                .filter(provider -> provider.getType() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "AI provider not found: " + type
                ));
    }
}
package com.investnavigator.backend.ai.controller;

import com.investnavigator.backend.ai.config.AIProperties;
import com.investnavigator.backend.ai.provider.AIProvider;
import com.investnavigator.backend.ai.provider.AIProviderRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/provider")
@RequiredArgsConstructor
public class AIProviderController {

    private final AIProperties aiProperties;
    private final AIProviderRegistry aiProviderRegistry;

    @GetMapping
    public Map<String, Object> getActiveProvider() {
        AIProvider provider = aiProviderRegistry.getProvider(aiProperties.provider());

        return Map.of(
                "activeProvider", provider.getType(),
                "status", "AVAILABLE"
        );
    }
}
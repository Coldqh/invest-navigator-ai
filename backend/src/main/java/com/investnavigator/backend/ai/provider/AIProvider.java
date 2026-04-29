package com.investnavigator.backend.ai.provider;

import com.investnavigator.backend.ai.provider.dto.AIAnalysisRequest;
import com.investnavigator.backend.ai.provider.dto.AIAnalysisResult;

public interface AIProvider {

    AIProviderType getType();

    AIAnalysisResult analyze(AIAnalysisRequest request);
}
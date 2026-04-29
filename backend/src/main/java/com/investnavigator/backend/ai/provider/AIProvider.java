package com.investnavigator.backend.ai.provider;

import com.investnavigator.backend.ai.provider.dto.AIAnalysisRequest;
import com.investnavigator.backend.ai.provider.dto.AIAnalysisResult;
import com.investnavigator.backend.ai.provider.dto.AIPortfolioAnalysisRequest;

public interface AIProvider {

    AIProviderType getType();

    AIAnalysisResult analyze(AIAnalysisRequest request);

    AIAnalysisResult analyzePortfolio(AIPortfolioAnalysisRequest request);
}
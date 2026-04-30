package com.investnavigator.backend.ai.provider;

import com.investnavigator.backend.ai.provider.dto.AIAnalysisRequest;
import com.investnavigator.backend.ai.provider.dto.AIAnalysisResult;
import com.investnavigator.backend.ai.provider.dto.AICompareAnalysisRequest;
import com.investnavigator.backend.ai.provider.dto.AIPortfolioAnalysisRequest;
import com.investnavigator.backend.ai.provider.dto.AIWatchlistAnalysisRequest;

public interface AIProvider {

    AIProviderType getType();

    AIAnalysisResult analyze(AIAnalysisRequest request);

    AIAnalysisResult analyzePortfolio(AIPortfolioAnalysisRequest request);

    AIAnalysisResult analyzeWatchlist(AIWatchlistAnalysisRequest request);

    AIAnalysisResult analyzeCompare(AICompareAnalysisRequest request);
}
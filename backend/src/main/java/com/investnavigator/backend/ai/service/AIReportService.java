package com.investnavigator.backend.ai.service;

import com.investnavigator.backend.ai.config.AIProperties;
import com.investnavigator.backend.ai.dto.AIReportResponse;
import com.investnavigator.backend.ai.mapper.AIReportMapper;
import com.investnavigator.backend.ai.model.AIReport;
import com.investnavigator.backend.ai.provider.AIProvider;
import com.investnavigator.backend.ai.provider.AIProviderRegistry;
import com.investnavigator.backend.ai.provider.dto.AIAnalysisRequest;
import com.investnavigator.backend.ai.provider.dto.AIAnalysisResult;
import com.investnavigator.backend.ai.repository.AIReportRepository;
import com.investnavigator.backend.analytics.dto.AnalyticsSummaryResponse;
import com.investnavigator.backend.analytics.service.AnalyticsService;
import com.investnavigator.backend.asset.model.Asset;
import com.investnavigator.backend.asset.repository.AssetRepository;
import com.investnavigator.backend.common.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AIReportService {

    private final AssetRepository assetRepository;
    private final AIReportRepository aiReportRepository;
    private final AIReportMapper aiReportMapper;
    private final AnalyticsService analyticsService;
    private final AIProperties aiProperties;
    private final AIProviderRegistry aiProviderRegistry;

    @Transactional
    public AIReportResponse generateReport(String ticker) {
        Asset asset = assetRepository.findByTickerIgnoreCase(ticker)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + ticker));

        AnalyticsSummaryResponse analytics = analyticsService.getSummary(ticker);

        AIProvider aiProvider = aiProviderRegistry.getProvider(aiProperties.provider());

        AIAnalysisResult analysisResult = aiProvider.analyze(
                new AIAnalysisRequest(
                        asset.getTicker(),
                        asset.getName(),
                        analytics
                )
        );

        aiReportRepository.deleteByAsset(asset);
        aiReportRepository.flush();

        AIReport report = AIReport.builder()
                .asset(asset)
                .summary(analysisResult.summary())
                .positiveFactors(analysisResult.positiveFactors())
                .negativeFactors(analysisResult.negativeFactors())
                .riskLevel(analysisResult.riskLevel())
                .riskScore(analysisResult.riskScore())
                .confidence(analysisResult.confidence())
                .explanation(analysisResult.explanation())
                .disclaimer(analysisResult.disclaimer())
                .build();

        AIReport savedReport = aiReportRepository.save(report);

        return aiReportMapper.toResponse(savedReport);
    }

    @Transactional(readOnly = true)
    public List<AIReportResponse> getReportsByTicker(String ticker) {
        Asset asset = assetRepository.findByTickerIgnoreCase(ticker)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + ticker));

        return aiReportRepository.findTopByAssetOrderByCreatedAtDesc(asset)
                .map(aiReportMapper::toResponse)
                .map(List::of)
                .orElseGet(List::of);
    }

    @Transactional(readOnly = true)
    public AIReportResponse getReportById(UUID reportId) {
        return aiReportRepository.findById(reportId)
                .map(aiReportMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("AI report not found: " + reportId));
    }
}
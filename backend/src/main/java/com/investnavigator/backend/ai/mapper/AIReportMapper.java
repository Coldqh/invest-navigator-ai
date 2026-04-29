package com.investnavigator.backend.ai.mapper;

import com.investnavigator.backend.ai.dto.AIReportResponse;
import com.investnavigator.backend.ai.model.AIReport;
import com.investnavigator.backend.ai.provider.AIProviderType;
import com.investnavigator.backend.asset.model.Asset;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AIReportMapper {

    public AIReportResponse toResponse(AIReport report) {
        return toResponse(report, null);
    }

    public AIReportResponse toResponse(
            AIReport report,
            String fallbackReason
    ) {
        Asset asset = report.getAsset();

        return new AIReportResponse(
                report.getId(),
                asset.getId(),
                asset.getTicker(),
                asset.getName(),
                report.getAiProvider() == null ? AIProviderType.MOCK : report.getAiProvider(),
                report.getSummary(),
                safeList(report.getPositiveFactors()),
                safeList(report.getNegativeFactors()),
                report.getRiskLevel(),
                report.getRiskScore(),
                report.getConfidence(),
                report.getExplanation(),
                report.getDisclaimer(),
                fallbackReason,
                report.getCreatedAt()
        );
    }

    private List<String> safeList(List<String> values) {
        if (values == null) {
            return List.of();
        }

        return List.copyOf(values);
    }
}
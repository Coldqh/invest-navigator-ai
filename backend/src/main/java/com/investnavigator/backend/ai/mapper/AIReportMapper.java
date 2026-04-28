package com.investnavigator.backend.ai.mapper;

import com.investnavigator.backend.ai.dto.AIReportResponse;
import com.investnavigator.backend.ai.model.AIReport;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AIReportMapper {

    public AIReportResponse toResponse(AIReport report) {
        return new AIReportResponse(
                report.getId(),
                report.getAsset().getId(),
                report.getAsset().getTicker(),
                report.getAsset().getName(),
                report.getSummary(),
                copyList(report.getPositiveFactors()),
                copyList(report.getNegativeFactors()),
                report.getRiskLevel(),
                report.getRiskScore(),
                report.getConfidence(),
                report.getExplanation(),
                report.getDisclaimer(),
                report.getCreatedAt()
        );
    }

    private List<String> copyList(List<String> source) {
        if (source == null) {
            return List.of();
        }

        return List.copyOf(source);
    }
}
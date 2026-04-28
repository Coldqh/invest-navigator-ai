package com.investnavigator.backend.ai.service;

import com.investnavigator.backend.ai.dto.AIReportResponse;
import com.investnavigator.backend.ai.mapper.AIReportMapper;
import com.investnavigator.backend.ai.model.AIReport;
import com.investnavigator.backend.ai.repository.AIReportRepository;
import com.investnavigator.backend.analytics.dto.AnalyticsSummaryResponse;
import com.investnavigator.backend.analytics.model.RiskLevel;
import com.investnavigator.backend.analytics.service.AnalyticsService;
import com.investnavigator.backend.asset.model.Asset;
import com.investnavigator.backend.asset.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AIReportService {

    private static final String DISCLAIMER = """
            Материал носит информационно-аналитический характер и не является индивидуальной инвестиционной рекомендацией.
            Решение о покупке, продаже или удержании финансового инструмента пользователь принимает самостоятельно.
            """;

    private final AssetRepository assetRepository;
    private final AIReportRepository aiReportRepository;
    private final AnalyticsService analyticsService;
    private final AIReportMapper aiReportMapper;

    @Transactional
    public AIReportResponse generateReport(String ticker) {
        Asset asset = assetRepository.findByTickerIgnoreCase(ticker)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + ticker));

        AnalyticsSummaryResponse analytics = analyticsService.getSummary(ticker);

        AIReport report = AIReport.builder()
                .asset(asset)
                .summary(buildSummary(analytics))
                .positiveFactors(buildPositiveFactors(analytics))
                .negativeFactors(buildNegativeFactors(analytics))
                .riskLevel(analytics.riskLevel())
                .riskScore(analytics.riskScore())
                .confidence(calculateConfidence(analytics))
                .explanation(buildExplanation(analytics))
                .disclaimer(DISCLAIMER)
                .build();

        AIReport savedReport = aiReportRepository.save(report);

        return aiReportMapper.toResponse(savedReport);
    }

    @Transactional(readOnly = true)
    public List<AIReportResponse> getReportsByTicker(String ticker) {
        Asset asset = assetRepository.findByTickerIgnoreCase(ticker)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + ticker));

        return aiReportRepository.findByAssetOrderByCreatedAtDesc(asset)
                .stream()
                .map(aiReportMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AIReportResponse getReportById(UUID reportId) {
        return aiReportRepository.findById(reportId)
                .map(aiReportMapper::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("AI report not found: " + reportId));
    }

    private String buildSummary(AnalyticsSummaryResponse analytics) {
        return switch (analytics.riskLevel()) {
            case LOW -> "Актив демонстрирует низкий уровень расчетного риска на основе доступных данных. Динамика выглядит относительно стабильной, но вывод ограничен объемом исторических данных.";
            case MEDIUM -> "Актив имеет умеренный уровень расчетного риска. Движение цены и волатильность требуют дополнительного наблюдения, но критических сигналов на текущем наборе данных не выявлено.";
            case HIGH -> "Актив имеет повышенный уровень расчетного риска. Система фиксирует заметную волатильность или существенное изменение цены, поэтому требуется более осторожный анализ.";
            case CRITICAL -> "Актив имеет критический уровень расчетного риска. Доступные данные указывают на резкую динамику или нестабильность, поэтому вывод требует дополнительной проверки.";
        };
    }

    private List<String> buildPositiveFactors(AnalyticsSummaryResponse analytics) {
        List<String> factors = new ArrayList<>();

        if (analytics.priceChangePercent().compareTo(BigDecimal.ZERO) >= 0) {
            factors.add("Цена закрытия за анализируемый период выросла на " + analytics.priceChangePercent() + "%.");
        } else {
            factors.add("Несмотря на снижение цены, динамика зафиксирована и может использоваться для дальнейшего сравнения с другими активами.");
        }

        factors.add("Средний объем торгов за период составляет " + analytics.averageVolume() + ".");

        if (analytics.riskLevel() == RiskLevel.LOW || analytics.riskLevel() == RiskLevel.MEDIUM) {
            factors.add("Расчетный риск не превышает умеренный уровень по текущей модели.");
        }

        return factors;
    }

    private List<String> buildNegativeFactors(AnalyticsSummaryResponse analytics) {
        List<String> factors = new ArrayList<>();

        if (analytics.dataPoints() < 5) {
            factors.add("История данных ограничена: использовано только " + analytics.dataPoints() + " свечей, поэтому надежность вывода снижена.");
        }

        if (analytics.volatilityPercent().compareTo(BigDecimal.valueOf(2)) >= 0) {
            factors.add("Волатильность за период составляет " + analytics.volatilityPercent() + "%, что повышает расчетный риск.");
        }

        if (analytics.priceChangePercent().abs().compareTo(BigDecimal.valueOf(5)) >= 0) {
            factors.add("Изменение цены за период превышает 5%, что может указывать на повышенную рыночную чувствительность.");
        }

        if (factors.isEmpty()) {
            factors.add("Существенных негативных факторов на текущем наборе данных не выявлено.");
        }

        return factors;
    }

    private BigDecimal calculateConfidence(AnalyticsSummaryResponse analytics) {
        BigDecimal confidence = BigDecimal.valueOf(0.55);

        if (analytics.dataPoints() >= 5) {
            confidence = confidence.add(BigDecimal.valueOf(0.15));
        }

        if (analytics.dataPoints() >= 15) {
            confidence = confidence.add(BigDecimal.valueOf(0.15));
        }

        if (analytics.riskLevel() == RiskLevel.LOW || analytics.riskLevel() == RiskLevel.MEDIUM) {
            confidence = confidence.add(BigDecimal.valueOf(0.05));
        }

        return confidence.min(BigDecimal.valueOf(0.90))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String buildExplanation(AnalyticsSummaryResponse analytics) {
        return """
                Отчет сформирован на основе текущей цены, дневных свечей, изменения цены, среднего объема, расчетной волатильности и риск-скора.
                Итоговый риск-уровень: %s.
                Риск-скор: %d из 100.
                Изменение цены за период: %s%%.
                Волатильность за период: %s%%.
                Количество использованных точек данных: %d.
                """
                .formatted(
                        analytics.riskLevel(),
                        analytics.riskScore(),
                        analytics.priceChangePercent(),
                        analytics.volatilityPercent(),
                        analytics.dataPoints()
                );
    }
}
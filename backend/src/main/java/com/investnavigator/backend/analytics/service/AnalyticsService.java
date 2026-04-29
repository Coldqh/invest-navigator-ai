package com.investnavigator.backend.analytics.service;

import com.investnavigator.backend.analytics.dto.AnalyticsSummaryResponse;
import com.investnavigator.backend.analytics.model.RiskLevel;
import com.investnavigator.backend.common.error.BadRequestException;
import com.investnavigator.backend.common.error.ResourceNotFoundException;
import com.investnavigator.backend.marketdata.dto.CandleResponse;
import com.investnavigator.backend.marketdata.dto.MarketPriceResponse;
import com.investnavigator.backend.marketdata.model.Timeframe;
import com.investnavigator.backend.marketdata.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final int SCALE = 6;

    private final MarketDataService marketDataService;

    public AnalyticsSummaryResponse getSummary(String ticker) {
        MarketPriceResponse currentMarketPrice = marketDataService.getLatestMarketPrice(ticker);

        List<CandleResponse> candles = marketDataService.getCandles(ticker, Timeframe.ONE_DAY)
                .stream()
                .sorted(Comparator.comparing(CandleResponse::timestamp))
                .toList();

        if (candles.isEmpty()) {
            throw new ResourceNotFoundException("Candles not found for asset: " + ticker);
        }

        BigDecimal firstClose = candles.get(0).close();
        BigDecimal lastClose = candles.get(candles.size() - 1).close();

        BigDecimal priceChange = lastClose
                .subtract(firstClose)
                .setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal priceChangePercent = calculatePercentChange(firstClose, lastClose);
        BigDecimal averageVolume = calculateAverageVolume(candles);
        BigDecimal volatilityPercent = calculateVolatilityPercent(candles);

        int riskScore = calculateRiskScore(
                volatilityPercent,
                priceChangePercent,
                candles.size()
        );

        RiskLevel riskLevel = defineRiskLevel(riskScore);

        return new AnalyticsSummaryResponse(
                currentMarketPrice.ticker(),
                currentMarketPrice.name(),
                currentMarketPrice.price(),
                firstClose,
                lastClose,
                priceChange,
                priceChangePercent,
                averageVolume,
                volatilityPercent,
                riskScore,
                riskLevel,
                candles.size()
        );
    }

    public List<AnalyticsSummaryResponse> compareAssets(List<String> tickers) {
        if (tickers == null) {
            throw new BadRequestException("Tickers list is required");
        }

        List<String> normalizedTickers = tickers.stream()
                .filter(ticker -> ticker != null && !ticker.isBlank())
                .map(ticker -> ticker.trim().toUpperCase())
                .distinct()
                .toList();

        if (normalizedTickers.size() < 2) {
            throw new BadRequestException("You must compare at least 2 different assets");
        }

        if (normalizedTickers.size() > 5) {
            throw new BadRequestException("You can compare no more than 5 assets");
        }

        return normalizedTickers.stream()
                .map(this::getSummary)
                .toList();
    }

    private BigDecimal calculatePercentChange(BigDecimal firstClose, BigDecimal lastClose) {
        if (firstClose.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }

        return lastClose
                .subtract(firstClose)
                .divide(firstClose, SCALE + 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAverageVolume(List<CandleResponse> candles) {
        BigDecimal totalVolume = candles.stream()
                .map(CandleResponse::volume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalVolume.divide(
                BigDecimal.valueOf(candles.size()),
                SCALE,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal calculateVolatilityPercent(List<CandleResponse> candles) {
        if (candles.size() < 2) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }

        List<BigDecimal> returns = calculateDailyReturns(candles);

        BigDecimal averageReturn = returns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), SCALE + 4, RoundingMode.HALF_UP);

        BigDecimal variance = returns.stream()
                .map(value -> value.subtract(averageReturn))
                .map(diff -> diff.multiply(diff))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), SCALE + 4, RoundingMode.HALF_UP);

        double volatility = Math.sqrt(variance.doubleValue());

        return BigDecimal.valueOf(volatility)
                .multiply(BigDecimal.valueOf(100))
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    private List<BigDecimal> calculateDailyReturns(List<CandleResponse> candles) {
        return java.util.stream.IntStream.range(1, candles.size())
                .mapToObj(index -> {
                    BigDecimal previousClose = candles.get(index - 1).close();
                    BigDecimal currentClose = candles.get(index).close();

                    if (previousClose.compareTo(BigDecimal.ZERO) == 0) {
                        return BigDecimal.ZERO;
                    }

                    return currentClose
                            .subtract(previousClose)
                            .divide(previousClose, SCALE + 4, RoundingMode.HALF_UP);
                })
                .toList();
    }

    private int calculateRiskScore(
            BigDecimal volatilityPercent,
            BigDecimal priceChangePercent,
            int dataPoints
    ) {
        int score = 0;

        score += volatilityPercent
                .multiply(BigDecimal.valueOf(10))
                .intValue();

        BigDecimal absolutePriceChange = priceChangePercent.abs();

        if (absolutePriceChange.compareTo(BigDecimal.valueOf(10)) >= 0) {
            score += 30;
        } else if (absolutePriceChange.compareTo(BigDecimal.valueOf(5)) >= 0) {
            score += 20;
        } else if (absolutePriceChange.compareTo(BigDecimal.valueOf(2)) >= 0) {
            score += 10;
        }

        if (dataPoints < 5) {
            score += 20;
        }

        return Math.min(score, 100);
    }

    private RiskLevel defineRiskLevel(int riskScore) {
        if (riskScore >= 80) {
            return RiskLevel.CRITICAL;
        }

        if (riskScore >= 60) {
            return RiskLevel.HIGH;
        }

        if (riskScore >= 30) {
            return RiskLevel.MEDIUM;
        }

        return RiskLevel.LOW;
    }
}
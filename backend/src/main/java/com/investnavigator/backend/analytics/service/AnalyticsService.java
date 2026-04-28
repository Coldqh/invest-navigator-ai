package com.investnavigator.backend.analytics.service;

import com.investnavigator.backend.analytics.dto.AnalyticsSummaryResponse;
import com.investnavigator.backend.analytics.model.RiskLevel;
import com.investnavigator.backend.asset.model.Asset;
import com.investnavigator.backend.asset.repository.AssetRepository;
import com.investnavigator.backend.marketdata.model.Candle;
import com.investnavigator.backend.marketdata.model.MarketPrice;
import com.investnavigator.backend.marketdata.model.Timeframe;
import com.investnavigator.backend.marketdata.repository.CandleRepository;
import com.investnavigator.backend.marketdata.repository.MarketPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.investnavigator.backend.common.error.ResourceNotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final int SCALE = 6;

    private final AssetRepository assetRepository;
    private final MarketPriceRepository marketPriceRepository;
    private final CandleRepository candleRepository;

    public AnalyticsSummaryResponse getSummary(String ticker) {
        Asset asset = assetRepository.findByTickerIgnoreCase(ticker)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + ticker));

        MarketPrice currentMarketPrice = marketPriceRepository.findTopByAssetOrderByTimestampDesc(asset)
                .orElseThrow(() -> new ResourceNotFoundException("Market price not found for asset: " + ticker));

        List<Candle> candles = candleRepository
                .findTop30ByAssetAndTimeframeOrderByTimestampDesc(asset, Timeframe.ONE_DAY)
                .stream()
                .sorted(Comparator.comparing(Candle::getTimestamp))
                .toList();

        if (candles.isEmpty()) {
            throw new ResourceNotFoundException("Candles not found for asset: " + ticker);
        }

        BigDecimal firstClose = candles.getFirst().getClose();
        BigDecimal lastClose = candles.getLast().getClose();

        BigDecimal priceChange = lastClose.subtract(firstClose).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal priceChangePercent = calculatePercentChange(firstClose, lastClose);

        BigDecimal averageVolume = calculateAverageVolume(candles);
        BigDecimal volatilityPercent = calculateVolatilityPercent(candles);

        int riskScore = calculateRiskScore(volatilityPercent, priceChangePercent, candles.size());
        RiskLevel riskLevel = defineRiskLevel(riskScore);

        return new AnalyticsSummaryResponse(
                asset.getTicker(),
                asset.getName(),
                currentMarketPrice.getPrice(),
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

    private BigDecimal calculateAverageVolume(List<Candle> candles) {
        BigDecimal totalVolume = candles.stream()
                .map(Candle::getVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalVolume.divide(
                BigDecimal.valueOf(candles.size()),
                SCALE,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal calculateVolatilityPercent(List<Candle> candles) {
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

    private List<BigDecimal> calculateDailyReturns(List<Candle> candles) {
        return java.util.stream.IntStream.range(1, candles.size())
                .mapToObj(index -> {
                    BigDecimal previousClose = candles.get(index - 1).getClose();
                    BigDecimal currentClose = candles.get(index).getClose();

                    if (previousClose.compareTo(BigDecimal.ZERO) == 0) {
                        return BigDecimal.ZERO;
                    }

                    return currentClose
                            .subtract(previousClose)
                            .divide(previousClose, SCALE + 4, RoundingMode.HALF_UP);
                })
                .toList();
    }

    private int calculateRiskScore(BigDecimal volatilityPercent, BigDecimal priceChangePercent, int dataPoints) {
        int score = 0;

        score += volatilityPercent.multiply(BigDecimal.valueOf(10)).intValue();

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
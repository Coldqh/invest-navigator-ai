package com.investnavigator.backend.portfolio.mapper;

import com.investnavigator.backend.asset.model.Asset;
import com.investnavigator.backend.marketdata.dto.MarketPriceResponse;
import com.investnavigator.backend.portfolio.dto.PortfolioPositionResponse;
import com.investnavigator.backend.portfolio.model.PortfolioPosition;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PortfolioPositionMapper {

    private static final int MONEY_SCALE = 6;
    private static final int PERCENT_SCALE = 6;

    public PortfolioPositionResponse toResponse(
            PortfolioPosition position,
            MarketPriceResponse marketPrice
    ) {
        Asset asset = position.getAsset();

        BigDecimal quantity = safe(position.getQuantity());
        BigDecimal averageBuyPrice = safe(position.getAverageBuyPrice());
        BigDecimal currentPrice = safe(marketPrice.price());

        BigDecimal investedAmount = quantity
                .multiply(averageBuyPrice)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal currentValue = quantity
                .multiply(currentPrice)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal profitLoss = currentValue
                .subtract(investedAmount)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal profitLossPercent = calculateProfitLossPercent(
                investedAmount,
                profitLoss
        );

        return new PortfolioPositionResponse(
                position.getId(),
                asset.getId(),
                asset.getTicker(),
                asset.getName(),
                asset.getAssetType(),
                asset.getExchange(),
                asset.getCurrency(),
                quantity,
                averageBuyPrice,
                investedAmount,
                currentPrice,
                currentValue,
                profitLoss,
                profitLossPercent,
                marketPrice.source(),
                marketPrice.timestamp(),
                position.getCreatedAt(),
                position.getUpdatedAt()
        );
    }

    private BigDecimal calculateProfitLossPercent(
            BigDecimal investedAmount,
            BigDecimal profitLoss
    ) {
        if (investedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(PERCENT_SCALE, RoundingMode.HALF_UP);
        }

        return profitLoss
                .divide(investedAmount, PERCENT_SCALE + 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(PERCENT_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
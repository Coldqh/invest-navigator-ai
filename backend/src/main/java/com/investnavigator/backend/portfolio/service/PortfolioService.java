package com.investnavigator.backend.portfolio.service;

import com.investnavigator.backend.asset.model.Asset;
import com.investnavigator.backend.asset.repository.AssetRepository;
import com.investnavigator.backend.common.error.BadRequestException;
import com.investnavigator.backend.common.error.ResourceNotFoundException;
import com.investnavigator.backend.marketdata.dto.MarketPriceResponse;
import com.investnavigator.backend.marketdata.service.MarketDataService;
import com.investnavigator.backend.portfolio.dto.PortfolioPositionRequest;
import com.investnavigator.backend.portfolio.dto.PortfolioPositionResponse;
import com.investnavigator.backend.portfolio.dto.PortfolioSummaryResponse;
import com.investnavigator.backend.portfolio.mapper.PortfolioPositionMapper;
import com.investnavigator.backend.portfolio.model.PortfolioPosition;
import com.investnavigator.backend.portfolio.repository.PortfolioPositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private static final int MONEY_SCALE = 6;
    private static final int PERCENT_SCALE = 6;

    private final AssetRepository assetRepository;
    private final PortfolioPositionRepository portfolioPositionRepository;
    private final PortfolioPositionMapper portfolioPositionMapper;
    private final MarketDataService marketDataService;

    @Transactional(readOnly = true)
    public PortfolioSummaryResponse getPortfolio() {
        List<PortfolioPositionResponse> positions = portfolioPositionRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toPositionResponse)
                .toList();

        BigDecimal totalInvested = positions.stream()
                .map(PortfolioPositionResponse::investedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal totalCurrentValue = positions.stream()
                .map(PortfolioPositionResponse::currentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal totalProfitLoss = totalCurrentValue
                .subtract(totalInvested)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal totalProfitLossPercent = calculateTotalProfitLossPercent(
                totalInvested,
                totalProfitLoss
        );

        return new PortfolioSummaryResponse(
                positions.size(),
                totalInvested,
                totalCurrentValue,
                totalProfitLoss,
                totalProfitLossPercent,
                positions,
                Instant.now()
        );
    }

    @Transactional
    public PortfolioPositionResponse savePosition(PortfolioPositionRequest request) {
        validateRequest(request);

        Asset asset = assetRepository.findByTickerIgnoreCase(request.ticker())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Asset not found: " + request.ticker()
                ));

        PortfolioPosition position = portfolioPositionRepository.findByAsset(asset)
                .orElseGet(() -> PortfolioPosition.builder()
                        .asset(asset)
                        .build());

        position.setQuantity(request.quantity());
        position.setAverageBuyPrice(request.averageBuyPrice());

        PortfolioPosition savedPosition = portfolioPositionRepository.save(position);

        return toPositionResponse(savedPosition);
    }

    @Transactional
    public void deletePosition(String ticker) {
        Asset asset = assetRepository.findByTickerIgnoreCase(ticker)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + ticker));

        if (portfolioPositionRepository.existsByAsset(asset)) {
            portfolioPositionRepository.deleteByAsset(asset);
        }
    }

    private PortfolioPositionResponse toPositionResponse(PortfolioPosition position) {
        MarketPriceResponse marketPrice = marketDataService.getLatestMarketPrice(
                position.getAsset().getTicker()
        );

        return portfolioPositionMapper.toResponse(position, marketPrice);
    }

    private void validateRequest(PortfolioPositionRequest request) {
        if (request == null) {
            throw new BadRequestException("Portfolio position request is required");
        }

        if (request.ticker() == null || request.ticker().isBlank()) {
            throw new BadRequestException("Ticker is required");
        }

        if (request.quantity() == null || request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }

        if (request.averageBuyPrice() == null
                || request.averageBuyPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Average buy price must be greater than zero");
        }
    }

    private BigDecimal calculateTotalProfitLossPercent(
            BigDecimal totalInvested,
            BigDecimal totalProfitLoss
    ) {
        if (totalInvested.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(PERCENT_SCALE, RoundingMode.HALF_UP);
        }

        return totalProfitLoss
                .divide(totalInvested, PERCENT_SCALE + 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(PERCENT_SCALE, RoundingMode.HALF_UP);
    }
}
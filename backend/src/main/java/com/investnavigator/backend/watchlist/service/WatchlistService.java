package com.investnavigator.backend.watchlist.service;

import com.investnavigator.backend.asset.model.Asset;
import com.investnavigator.backend.asset.repository.AssetRepository;
import com.investnavigator.backend.common.error.ResourceNotFoundException;
import com.investnavigator.backend.marketdata.dto.MarketPriceResponse;
import com.investnavigator.backend.marketdata.service.MarketDataRefreshService;
import com.investnavigator.backend.watchlist.dto.WatchlistItemResponse;
import com.investnavigator.backend.watchlist.dto.WatchlistRefreshItemResponse;
import com.investnavigator.backend.watchlist.dto.WatchlistRefreshResponse;
import com.investnavigator.backend.watchlist.mapper.WatchlistItemMapper;
import com.investnavigator.backend.watchlist.model.WatchlistItem;
import com.investnavigator.backend.watchlist.repository.WatchlistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final AssetRepository assetRepository;
    private final WatchlistItemRepository watchlistItemRepository;
    private final WatchlistItemMapper watchlistItemMapper;
    private final MarketDataRefreshService marketDataRefreshService;

    @Transactional(readOnly = true)
    public List<WatchlistItemResponse> getWatchlist() {
        return watchlistItemRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(watchlistItemMapper::toResponse)
                .toList();
    }

    @Transactional
    public WatchlistItemResponse addToWatchlist(String ticker) {
        Asset asset = findAssetByTicker(ticker);

        return watchlistItemRepository.findByAsset(asset)
                .map(watchlistItemMapper::toResponse)
                .orElseGet(() -> createWatchlistItem(asset));
    }

    @Transactional
    public void removeFromWatchlist(String ticker) {
        Asset asset = findAssetByTicker(ticker);

        if (watchlistItemRepository.existsByAsset(asset)) {
            watchlistItemRepository.deleteByAsset(asset);
        }
    }

    @Transactional
    public WatchlistRefreshResponse refreshWatchlist() {
        List<WatchlistItem> items = watchlistItemRepository.findAllByOrderByCreatedAtDesc();

        List<WatchlistRefreshItemResponse> refreshResults = items.stream()
                .map(this::refreshWatchlistItem)
                .toList();

        int refreshedItems = (int) refreshResults.stream()
                .filter(WatchlistRefreshItemResponse::refreshed)
                .count();

        int failedItems = refreshResults.size() - refreshedItems;

        return new WatchlistRefreshResponse(
                refreshResults.size(),
                refreshedItems,
                failedItems,
                refreshResults,
                Instant.now()
        );
    }

    private WatchlistRefreshItemResponse refreshWatchlistItem(WatchlistItem item) {
        Asset asset = item.getAsset();

        try {
            MarketPriceResponse refreshedPrice = marketDataRefreshService.refreshLatestPrice(
                    asset.getTicker()
            );

            return new WatchlistRefreshItemResponse(
                    asset.getTicker(),
                    asset.getName(),
                    true,
                    refreshedPrice.price(),
                    refreshedPrice.volume(),
                    refreshedPrice.source(),
                    refreshedPrice.timestamp(),
                    null
            );
        } catch (RuntimeException exception) {
            return new WatchlistRefreshItemResponse(
                    asset.getTicker(),
                    asset.getName(),
                    false,
                    null,
                    null,
                    null,
                    null,
                    exception.getMessage()
            );
        }
    }

    private WatchlistItemResponse createWatchlistItem(Asset asset) {
        WatchlistItem item = WatchlistItem.builder()
                .asset(asset)
                .build();

        WatchlistItem savedItem = watchlistItemRepository.save(item);

        return watchlistItemMapper.toResponse(savedItem);
    }

    private Asset findAssetByTicker(String ticker) {
        return assetRepository.findByTickerIgnoreCase(ticker)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + ticker));
    }
}
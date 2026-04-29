package com.investnavigator.backend.watchlist.service;

import com.investnavigator.backend.asset.model.Asset;
import com.investnavigator.backend.asset.repository.AssetRepository;
import com.investnavigator.backend.common.error.ResourceNotFoundException;
import com.investnavigator.backend.watchlist.dto.WatchlistItemResponse;
import com.investnavigator.backend.watchlist.mapper.WatchlistItemMapper;
import com.investnavigator.backend.watchlist.model.WatchlistItem;
import com.investnavigator.backend.watchlist.repository.WatchlistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final AssetRepository assetRepository;
    private final WatchlistItemRepository watchlistItemRepository;
    private final WatchlistItemMapper watchlistItemMapper;

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
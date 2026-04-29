package com.investnavigator.backend.watchlist.mapper;

import com.investnavigator.backend.asset.model.Asset;
import com.investnavigator.backend.watchlist.dto.WatchlistItemResponse;
import com.investnavigator.backend.watchlist.model.WatchlistItem;
import org.springframework.stereotype.Component;

@Component
public class WatchlistItemMapper {

    public WatchlistItemResponse toResponse(WatchlistItem item) {
        Asset asset = item.getAsset();

        return new WatchlistItemResponse(
                item.getId(),
                asset.getId(),
                asset.getTicker(),
                asset.getName(),
                asset.getAssetType(),
                asset.getExchange(),
                asset.getCurrency(),
                asset.getIsin(),
                asset.isActive(),
                item.getCreatedAt()
        );
    }
}
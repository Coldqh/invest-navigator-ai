package com.investnavigator.backend.watchlist.repository;

import com.investnavigator.backend.asset.model.Asset;
import com.investnavigator.backend.watchlist.model.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, UUID> {

    List<WatchlistItem> findAllByOrderByCreatedAtDesc();

    Optional<WatchlistItem> findByAsset(Asset asset);

    boolean existsByAsset(Asset asset);

    void deleteByAsset(Asset asset);
}
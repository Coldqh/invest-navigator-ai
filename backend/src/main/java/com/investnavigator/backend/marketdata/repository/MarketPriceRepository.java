package com.investnavigator.backend.marketdata.repository;

import com.investnavigator.backend.asset.model.Asset;
import com.investnavigator.backend.marketdata.model.MarketPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MarketPriceRepository extends JpaRepository<MarketPrice, UUID> {

    Optional<MarketPrice> findTopByAssetOrderByTimestampDesc(Asset asset);
}
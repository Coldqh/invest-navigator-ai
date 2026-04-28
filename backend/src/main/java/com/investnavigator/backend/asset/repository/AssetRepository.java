package com.investnavigator.backend.asset.repository;

import com.investnavigator.backend.asset.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<Asset, UUID> {

    Optional<Asset> findByTickerIgnoreCase(String ticker);

    List<Asset> findByTickerContainingIgnoreCaseOrNameContainingIgnoreCase(String ticker, String name);

    boolean existsByTickerIgnoreCaseAndExchangeIgnoreCase(String ticker, String exchange);
}
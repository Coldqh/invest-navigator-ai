package com.investnavigator.backend.portfolio.repository;

import com.investnavigator.backend.asset.model.Asset;
import com.investnavigator.backend.portfolio.model.PortfolioPosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioPositionRepository extends JpaRepository<PortfolioPosition, UUID> {

    List<PortfolioPosition> findAllByOrderByCreatedAtDesc();

    Optional<PortfolioPosition> findByAsset(Asset asset);

    boolean existsByAsset(Asset asset);

    void deleteByAsset(Asset asset);
}
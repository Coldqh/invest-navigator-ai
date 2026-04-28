package com.investnavigator.backend.ai.repository;

import com.investnavigator.backend.ai.model.AIReport;
import com.investnavigator.backend.asset.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AIReportRepository extends JpaRepository<AIReport, UUID> {

    List<AIReport> findByAssetOrderByCreatedAtDesc(Asset asset);
}
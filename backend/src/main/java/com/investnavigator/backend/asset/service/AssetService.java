package com.investnavigator.backend.asset.service;

import com.investnavigator.backend.asset.dto.AssetResponse;
import com.investnavigator.backend.asset.mapper.AssetMapper;
import com.investnavigator.backend.asset.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final AssetMapper assetMapper;

    public List<AssetResponse> getAllAssets() {
        return assetRepository.findAll()
                .stream()
                .map(assetMapper::toResponse)
                .toList();
    }

    public List<AssetResponse> searchAssets(String query) {
        if (query == null || query.isBlank()) {
            return getAllAssets();
        }

        return assetRepository
                .findByTickerContainingIgnoreCaseOrNameContainingIgnoreCase(query, query)
                .stream()
                .map(assetMapper::toResponse)
                .toList();
    }

    public AssetResponse getAssetByTicker(String ticker) {
        return assetRepository.findByTickerIgnoreCase(ticker)
                .map(assetMapper::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + ticker));
    }
}
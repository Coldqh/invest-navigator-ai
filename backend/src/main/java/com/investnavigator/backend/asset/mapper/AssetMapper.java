package com.investnavigator.backend.asset.mapper;

import com.investnavigator.backend.asset.dto.AssetResponse;
import com.investnavigator.backend.asset.model.Asset;
import org.springframework.stereotype.Component;

@Component
public class AssetMapper {

    public AssetResponse toResponse(Asset asset) {
        return new AssetResponse(
                asset.getId(),
                asset.getTicker(),
                asset.getName(),
                asset.getAssetType(),
                asset.getExchange(),
                asset.getCurrency(),
                asset.getIsin(),
                asset.isActive()
        );
    }
}
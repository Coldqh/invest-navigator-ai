package com.investnavigator.backend.asset.controller;

import com.investnavigator.backend.asset.dto.AssetResponse;
import com.investnavigator.backend.asset.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @GetMapping
    public List<AssetResponse> getAllAssets() {
        return assetService.getAllAssets();
    }

    @GetMapping("/search")
    public List<AssetResponse> searchAssets(@RequestParam(required = false) String query) {
        return assetService.searchAssets(query);
    }

    @GetMapping("/{ticker}")
    public AssetResponse getAssetByTicker(@PathVariable String ticker) {
        return assetService.getAssetByTicker(ticker);
    }
}
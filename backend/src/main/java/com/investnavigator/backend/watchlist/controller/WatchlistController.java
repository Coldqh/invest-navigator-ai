package com.investnavigator.backend.watchlist.controller;

import com.investnavigator.backend.watchlist.dto.WatchlistItemResponse;
import com.investnavigator.backend.watchlist.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @GetMapping
    public List<WatchlistItemResponse> getWatchlist() {
        return watchlistService.getWatchlist();
    }

    @PostMapping("/{ticker}")
    public WatchlistItemResponse addToWatchlist(@PathVariable String ticker) {
        return watchlistService.addToWatchlist(ticker);
    }

    @DeleteMapping("/{ticker}")
    public ResponseEntity<Void> removeFromWatchlist(@PathVariable String ticker) {
        watchlistService.removeFromWatchlist(ticker);

        return ResponseEntity.noContent().build();
    }
}
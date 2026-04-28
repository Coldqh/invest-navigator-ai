package com.investnavigator.backend.marketdata.dto;

import com.investnavigator.backend.marketdata.model.Timeframe;

import java.math.BigDecimal;
import java.time.Instant;

public record CandleResponse(
        Timeframe timeframe,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal volume,
        String source,
        Instant timestamp
) {
}
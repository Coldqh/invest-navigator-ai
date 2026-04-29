package com.investnavigator.backend.marketdata.provider.moex;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MoexResponse(
        MoexTableResponse securities,
        MoexTableResponse marketdata,
        MoexTableResponse candles
) {
}
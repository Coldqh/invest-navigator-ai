package com.investnavigator.backend.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CompareAssetsRequest(
        @NotNull(message = "Tickers list is required")
        @Size(min = 2, max = 5, message = "You must compare from 2 to 5 assets")
        List<@NotBlank(message = "Ticker must not be blank") String> tickers
) {
}
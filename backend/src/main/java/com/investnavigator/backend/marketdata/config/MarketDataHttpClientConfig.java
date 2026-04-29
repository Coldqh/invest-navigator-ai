package com.investnavigator.backend.marketdata.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class MarketDataHttpClientConfig {

    @Bean
    public RestClient binanceRestClient(MarketDataProperties marketDataProperties) {
        return RestClient.builder()
                .baseUrl(marketDataProperties.binance().baseUrl())
                .build();
    }
}
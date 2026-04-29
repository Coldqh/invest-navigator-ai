package com.investnavigator.backend.marketdata.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class MarketDataHttpClientConfig {

    @Bean
    @Qualifier("binanceRestClient")
    public RestClient binanceRestClient(MarketDataProperties marketDataProperties) {
        return RestClient.builder()
                .baseUrl(marketDataProperties.binance().baseUrl())
                .build();
    }

    @Bean
    @Qualifier("moexRestClient")
    public RestClient moexRestClient(MarketDataProperties marketDataProperties) {
        return RestClient.builder()
                .baseUrl(marketDataProperties.moex().baseUrl())
                .build();
    }
}
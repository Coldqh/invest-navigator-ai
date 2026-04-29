package com.investnavigator.backend.ai.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AIHttpClientConfig {

    @Bean
    @Qualifier("yandexGptRestClient")
    public RestClient yandexGptRestClient(AIProperties aiProperties) {
        return RestClient.builder()
                .baseUrl(aiProperties.yandexGpt().baseUrl())
                .build();
    }

    @Bean
    @Qualifier("gigaChatRestClient")
    public RestClient gigaChatRestClient(AIProperties aiProperties) {
        return RestClient.builder()
                .baseUrl(aiProperties.gigaChat().baseUrl())
                .build();
    }

    @Bean
    @Qualifier("openAiRestClient")
    public RestClient openAiRestClient(AIProperties aiProperties) {
        return RestClient.builder()
                .baseUrl(aiProperties.openAi().baseUrl())
                .build();
    }
}
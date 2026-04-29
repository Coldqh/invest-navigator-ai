package com.investnavigator.backend.ai.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class AIHttpClientConfig {

    @Bean
    @Qualifier("yandexGptRestClient")
    public RestClient yandexGptRestClient(AIProperties aiProperties) {
        return RestClient.builder()
                .baseUrl(aiProperties.yandexGpt().baseUrl())
                .requestFactory(createRequestFactory(aiProperties.yandexGpt().timeoutSeconds()))
                .build();
    }

    @Bean
    @Qualifier("gigaChatRestClient")
    public RestClient gigaChatRestClient(AIProperties aiProperties) {
        return RestClient.builder()
                .baseUrl(aiProperties.gigaChat().baseUrl())
                .requestFactory(createRequestFactory(aiProperties.gigaChat().timeoutSeconds()))
                .build();
    }

    @Bean
    @Qualifier("openAiRestClient")
    public RestClient openAiRestClient(AIProperties aiProperties) {
        return RestClient.builder()
                .baseUrl(aiProperties.openAi().baseUrl())
                .requestFactory(createRequestFactory(aiProperties.openAi().timeoutSeconds()))
                .build();
    }

    private SimpleClientHttpRequestFactory createRequestFactory(int timeoutSeconds) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(timeoutSeconds);

        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        return requestFactory;
    }
}
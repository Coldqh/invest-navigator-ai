package com.investnavigator.backend.ai.provider.yandex;

import com.investnavigator.backend.ai.config.AIProperties;
import com.investnavigator.backend.ai.provider.AIProvider;
import com.investnavigator.backend.ai.provider.AIProviderType;
import com.investnavigator.backend.ai.provider.dto.AIAnalysisRequest;
import com.investnavigator.backend.ai.provider.dto.AIAnalysisResult;
import com.investnavigator.backend.ai.provider.dto.AICompareAnalysisRequest;
import com.investnavigator.backend.ai.provider.dto.AIPortfolioAnalysisRequest;
import com.investnavigator.backend.ai.provider.dto.AIProviderPrompt;
import com.investnavigator.backend.ai.provider.dto.AIProviderRawResponse;
import com.investnavigator.backend.ai.provider.dto.AIWatchlistAnalysisRequest;
import com.investnavigator.backend.ai.provider.parser.AIProviderJsonParser;
import com.investnavigator.backend.ai.provider.prompt.AIProviderPromptBuilder;
import com.investnavigator.backend.common.error.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class YandexGptAIProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(YandexGptAIProvider.class);

    private static final String AUTHORIZATION_HEADER_PREFIX = "Bearer ";
    private static final String OPENAI_PROJECT_HEADER = "OpenAI-Project";
    private static final BigDecimal DEFAULT_TEMPERATURE = BigDecimal.valueOf(0.1);
    private static final int DEFAULT_MAX_TOKENS = 2000;

    private final AIProperties aiProperties;
    private final AIProviderPromptBuilder promptBuilder;
    private final AIProviderJsonParser jsonParser;
    private final RestClient yandexGptRestClient;

    public YandexGptAIProvider(
            AIProperties aiProperties,
            AIProviderPromptBuilder promptBuilder,
            AIProviderJsonParser jsonParser,
            @Qualifier("yandexGptRestClient") RestClient yandexGptRestClient
    ) {
        this.aiProperties = aiProperties;
        this.promptBuilder = promptBuilder;
        this.jsonParser = jsonParser;
        this.yandexGptRestClient = yandexGptRestClient;
    }

    @Override
    public AIProviderType getType() {
        return AIProviderType.YANDEX_GPT;
    }

    @Override
    public AIAnalysisResult analyze(AIAnalysisRequest request) {
        AIProviderPrompt prompt = promptBuilder.buildPrompt(request);
        String rawText = sendPrompt(prompt);

        AIProviderRawResponse rawResponse = new AIProviderRawResponse(
                getType(),
                aiProperties.yandexGpt().model(),
                rawText,
                Instant.now()
        );

        return jsonParser.parse(rawResponse.rawText(), request.analytics());
    }

    @Override
    public AIAnalysisResult analyzePortfolio(AIPortfolioAnalysisRequest request) {
        AIProviderPrompt prompt = promptBuilder.buildPortfolioPrompt(request);
        String rawText = sendPrompt(prompt);

        AIProviderRawResponse rawResponse = new AIProviderRawResponse(
                getType(),
                aiProperties.yandexGpt().model(),
                rawText,
                Instant.now()
        );

        return jsonParser.parsePortfolio(rawResponse.rawText(), request.portfolio());
    }

    @Override
    public AIAnalysisResult analyzeWatchlist(AIWatchlistAnalysisRequest request) {
        AIProviderPrompt prompt = promptBuilder.buildWatchlistPrompt(request);
        String rawText = sendPrompt(prompt);

        AIProviderRawResponse rawResponse = new AIProviderRawResponse(
                getType(),
                aiProperties.yandexGpt().model(),
                rawText,
                Instant.now()
        );

        return jsonParser.parseWatchlist(rawResponse.rawText(), request);
    }

    @Override
    public AIAnalysisResult analyzeCompare(AICompareAnalysisRequest request) {
        AIProviderPrompt prompt = promptBuilder.buildComparePrompt(request);
        String rawText = sendPrompt(prompt);

        AIProviderRawResponse rawResponse = new AIProviderRawResponse(
                getType(),
                aiProperties.yandexGpt().model(),
                rawText,
                Instant.now()
        );

        return jsonParser.parseCompare(rawResponse.rawText(), request);
    }

    private String sendPrompt(AIProviderPrompt prompt) {
        AIProperties.ExternalProvider providerProperties = aiProperties.yandexGpt();

        validateConfiguration(providerProperties);

        try {
            return sendPromptWithResponseFormat(prompt, buildYandexJsonSchemaResponseFormat());
        } catch (RestClientResponseException exception) {
            log.warn(
                    "YandexGPT structured JSON request failed. Status: {}. Body: {}. Retrying without response_format.",
                    exception.getStatusCode(),
                    exception.getResponseBodyAsString()
            );

            return sendPromptWithResponseFormat(prompt, null);
        }
    }

    private String sendPromptWithResponseFormat(
            AIProviderPrompt prompt,
            Map<String, Object> responseFormat
    ) {
        AIProperties.ExternalProvider providerProperties = aiProperties.yandexGpt();

        YandexGptChatCompletionRequest chatCompletionRequest = new YandexGptChatCompletionRequest(
                providerProperties.model(),
                List.of(
                        new YandexGptChatMessage("system", prompt.systemPrompt()),
                        new YandexGptChatMessage("user", prompt.userPrompt())
                ),
                DEFAULT_TEMPERATURE,
                DEFAULT_MAX_TOKENS,
                false,
                responseFormat
        );

        RestClient.RequestBodySpec requestSpec = yandexGptRestClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(
                        "Authorization",
                        AUTHORIZATION_HEADER_PREFIX + providerProperties.apiKey()
                );

        if (providerProperties.hasFolderId()) {
            requestSpec = requestSpec.header(
                    OPENAI_PROJECT_HEADER,
                    providerProperties.folderId()
            );
        }

        YandexGptChatCompletionResponse response = requestSpec
                .body(chatCompletionRequest)
                .retrieve()
                .body(YandexGptChatCompletionResponse.class);

        return response == null ? "" : response.firstMessageContent();
    }

    private Map<String, Object> buildYandexJsonSchemaResponseFormat() {
        return Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "summary", Map.of(
                                        "type", "string",
                                        "description", "Short investment analysis summary."
                                ),
                                "positiveFactors", Map.of(
                                        "type", "array",
                                        "items", Map.of(
                                                "type", "string"
                                        ),
                                        "description", "Positive factors detected in the provided metrics."
                                ),
                                "negativeFactors", Map.of(
                                        "type", "array",
                                        "items", Map.of(
                                                "type", "string"
                                        ),
                                        "description", "Negative factors detected in the provided metrics."
                                ),
                                "riskLevel", Map.of(
                                        "type", "string",
                                        "enum", List.of(
                                                "LOW",
                                                "MEDIUM",
                                                "HIGH",
                                                "CRITICAL"
                                        ),
                                        "description", "Overall risk level."
                                ),
                                "riskScore", Map.of(
                                        "type", "integer",
                                        "minimum", 0,
                                        "maximum", 100,
                                        "description", "Risk score from 0 to 100."
                                ),
                                "confidence", Map.of(
                                        "type", "number",
                                        "minimum", 0,
                                        "maximum", 1,
                                        "description", "Confidence from 0 to 1."
                                ),
                                "explanation", Map.of(
                                        "type", "string",
                                        "description", "Plain language explanation of the analysis."
                                ),
                                "disclaimer", Map.of(
                                        "type", "string",
                                        "description", "Educational disclaimer, not financial advice."
                                )
                        ),
                        "required", List.of(
                                "summary",
                                "positiveFactors",
                                "negativeFactors",
                                "riskLevel",
                                "riskScore",
                                "confidence",
                                "explanation",
                                "disclaimer"
                        )
                )
        );
    }

    private void validateConfiguration(AIProperties.ExternalProvider providerProperties) {
        if (!providerProperties.enabled()) {
            throw new BadRequestException("YandexGPT provider is disabled in application config");
        }

        if (!providerProperties.hasApiKey()) {
            throw new BadRequestException("YandexGPT api-key is not configured");
        }

        if (!providerProperties.hasModel()) {
            throw new BadRequestException("YandexGPT model is not configured");
        }
    }
}
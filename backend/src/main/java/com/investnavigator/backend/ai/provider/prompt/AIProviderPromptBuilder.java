package com.investnavigator.backend.ai.provider.prompt;

import com.investnavigator.backend.ai.provider.dto.AIAnalysisRequest;
import com.investnavigator.backend.ai.provider.dto.AICompareAnalysisRequest;
import com.investnavigator.backend.ai.provider.dto.AICompareAssetSnapshot;
import com.investnavigator.backend.ai.provider.dto.AIPortfolioAnalysisRequest;
import com.investnavigator.backend.ai.provider.dto.AIProviderPrompt;
import com.investnavigator.backend.ai.provider.dto.AIWatchlistAnalysisRequest;
import com.investnavigator.backend.ai.provider.dto.AIWatchlistItemSnapshot;
import com.investnavigator.backend.analytics.dto.AnalyticsSummaryResponse;
import com.investnavigator.backend.portfolio.dto.PortfolioPositionResponse;
import com.investnavigator.backend.portfolio.dto.PortfolioSummaryResponse;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class AIProviderPromptBuilder {

    public AIProviderPrompt buildPrompt(AIAnalysisRequest request) {
        return new AIProviderPrompt(
                buildSystemPrompt(),
                buildUserPrompt(request)
        );
    }

    public AIProviderPrompt buildPortfolioPrompt(AIPortfolioAnalysisRequest request) {
        return new AIProviderPrompt(
                buildSystemPrompt(),
                buildPortfolioUserPrompt(request)
        );
    }

    public AIProviderPrompt buildWatchlistPrompt(AIWatchlistAnalysisRequest request) {
        return new AIProviderPrompt(
                buildSystemPrompt(),
                buildWatchlistUserPrompt(request)
        );
    }

    public AIProviderPrompt buildComparePrompt(AICompareAnalysisRequest request) {
        return new AIProviderPrompt(
                buildSystemPrompt(),
                buildCompareUserPrompt(request)
        );
    }

    private String buildSystemPrompt() {
        return """
                Ты — инвестиционный аналитический помощник внутри учебного приложения для анализа активов и портфеля.
                
                Всегда отвечай на русском языке.
                Верни только валидный JSON-объект.
                Не используй markdown.
                Не используй code fence.
                Не добавляй текст перед JSON.
                Не добавляй текст после JSON.
                
                Обязательная структура JSON:
                {
                  "summary": "string",
                  "positiveFactors": ["string"],
                  "negativeFactors": ["string"],
                  "riskLevel": "LOW",
                  "riskScore": 0,
                  "confidence": 0.0,
                  "explanation": "string",
                  "disclaimer": "string"
                }
                
                Строгие правила:
                - summary должен быть коротким выводом на русском языке;
                - positiveFactors должен быть массивом строк на русском языке;
                - negativeFactors должен быть массивом строк на русском языке;
                - riskLevel должен быть строго одним из значений: LOW, MEDIUM, HIGH, CRITICAL;
                - riskScore должен быть целым числом от 0 до 100;
                - confidence должен быть числом от 0 до 1;
                - explanation должен объяснять рассуждение простым русским языком;
                - disclaimer должен быть на русском языке и говорить, что это учебный анализ, а не инвестиционная рекомендация;
                - не советуй покупать, продавать или держать актив;
                - не обещай доходность;
                - не выдавай анализ за профессиональную финансовую консультацию.
                """;
    }

    private String buildUserPrompt(AIAnalysisRequest request) {
        AnalyticsSummaryResponse analytics = request.analytics();

        return """
                Проанализируй актив по предоставленным рыночным метрикам.
                
                Актив:
                - Тикер: %s
                - Название: %s
                
                Рыночные метрики:
                - Текущая цена: %s
                - Первая цена закрытия в выборке: %s
                - Последняя цена закрытия в выборке: %s
                - Изменение цены: %s
                - Изменение цены в процентах: %s%%
                - Средний объём: %s
                - Волатильность в процентах: %s%%
                - Риск-скор: %s / 100
                - Уровень риска: %s
                - Количество точек данных: %s
                
                Сфокусируйся на:
                - динамике цены;
                - волатильности;
                - уровне риска;
                - качестве выборки данных;
                - понятном объяснении для начинающего инвестора.
                
                Верни только JSON по обязательной структуре.
                """.formatted(
                request.ticker(),
                request.name(),
                analytics.currentPrice(),
                analytics.firstClose(),
                analytics.lastClose(),
                analytics.priceChange(),
                analytics.priceChangePercent(),
                analytics.averageVolume(),
                analytics.volatilityPercent(),
                analytics.riskScore(),
                analytics.riskLevel(),
                analytics.dataPoints()
        );
    }

    private String buildPortfolioUserPrompt(AIPortfolioAnalysisRequest request) {
        PortfolioSummaryResponse portfolio = request.portfolio();

        String positionsText = portfolio.positions()
                .stream()
                .map(this::formatPortfolioPosition)
                .collect(Collectors.joining("\n"));

        return """
                Проанализируй инвестиционный портфель по предоставленным метрикам.
                
                Итоги портфеля:
                - Количество позиций: %s
                - Всего вложено: %s
                - Текущая стоимость: %s
                - Прибыль/убыток: %s
                - Прибыль/убыток в процентах: %s%%
                - Дата расчёта: %s
                
                Позиции портфеля:
                %s
                
                Сфокусируйся на:
                - концентрации портфеля;
                - текущей прибыли или убытке;
                - наиболее рискованных позициях;
                - балансе между криптовалютами и акциями;
                - общей устойчивости или спекулятивности портфеля;
                - понятном объяснении для начинающего инвестора.
                
                Верни только JSON по обязательной структуре.
                """.formatted(
                portfolio.positionsCount(),
                portfolio.totalInvested(),
                portfolio.totalCurrentValue(),
                portfolio.totalProfitLoss(),
                portfolio.totalProfitLossPercent(),
                portfolio.calculatedAt(),
                positionsText
        );
    }

    private String buildWatchlistUserPrompt(AIWatchlistAnalysisRequest request) {
        String itemsText = request.items()
                .stream()
                .map(this::formatWatchlistItem)
                .collect(Collectors.joining("\n"));

        return """
                Проанализируй watchlist по предоставленным рыночным данным.
                
                Watchlist:
                - Количество активов: %s
                - Дата генерации: %s
                
                Активы в watchlist:
                %s
                
                Сфокусируйся на:
                - какие активы выглядят более рискованными;
                - какие активы выглядят относительно стабильными;
                - балансе между криптовалютами и акциями;
                - качестве источников данных;
                - активах, за которыми стоит внимательнее наблюдать;
                - понятном объяснении для начинающего инвестора.
                
                Верни только JSON по обязательной структуре.
                """.formatted(
                request.items().size(),
                request.generatedAt(),
                itemsText
        );
    }

    private String buildCompareUserPrompt(AICompareAnalysisRequest request) {
        String assetsText = request.assets()
                .stream()
                .map(this::formatCompareAsset)
                .collect(Collectors.joining("\n"));

        return """
                Сравни активы по предоставленным аналитическим метрикам.
                
                Запрос на сравнение:
                - Количество активов: %s
                - Дата генерации: %s
                
                Активы:
                %s
                
                Сфокусируйся на:
                - какой актив выглядит более волатильным;
                - у какого актива сильнее движение цены;
                - у какого актива лучше профиль риска;
                - выглядит ли один из активов более спекулятивным;
                - качестве данных и размере выборки;
                - понятном объяснении для начинающего инвестора.
                
                Не говори пользователю покупать, продавать или держать актив.
                Верни только JSON по обязательной структуре.
                """.formatted(
                request.assets().size(),
                request.generatedAt(),
                assetsText
        );
    }

    private String formatPortfolioPosition(PortfolioPositionResponse position) {
        return """
                - %s:
                  название: %s
                  тип: %s
                  биржа: %s
                  валюта: %s
                  количество: %s
                  средняя цена покупки: %s
                  вложенная сумма: %s
                  текущая цена: %s
                  текущая стоимость: %s
                  прибыль/убыток: %s
                  прибыль/убыток в процентах: %s%%
                  источник цены: %s
                """.formatted(
                position.ticker(),
                position.name(),
                position.assetType(),
                position.exchange(),
                position.currency(),
                position.quantity(),
                position.averageBuyPrice(),
                position.investedAmount(),
                position.currentPrice(),
                position.currentValue(),
                position.profitLoss(),
                position.profitLossPercent(),
                position.priceSource()
        );
    }

    private String formatWatchlistItem(AIWatchlistItemSnapshot item) {
        return """
                - %s:
                  название: %s
                  тип: %s
                  биржа: %s
                  валюта: %s
                  последняя цена: %s
                  последний объём: %s
                  источник цены: %s
                  время цены: %s
                  ошибка данных: %s
                """.formatted(
                item.ticker(),
                item.name(),
                item.assetType(),
                item.exchange(),
                item.currency(),
                item.latestPrice(),
                item.latestVolume(),
                item.priceSource(),
                item.priceTimestamp(),
                item.dataError()
        );
    }

    private String formatCompareAsset(AICompareAssetSnapshot asset) {
        return """
                - %s:
                  название: %s
                  текущая цена: %s
                  первая цена закрытия в выборке: %s
                  последняя цена закрытия в выборке: %s
                  изменение цены: %s
                  изменение цены в процентах: %s%%
                  средний объём: %s
                  волатильность в процентах: %s%%
                  риск-скор: %s / 100
                  уровень риска: %s
                  количество точек данных: %s
                """.formatted(
                asset.ticker(),
                asset.name(),
                asset.currentPrice(),
                asset.firstClose(),
                asset.lastClose(),
                asset.priceChange(),
                asset.priceChangePercent(),
                asset.averageVolume(),
                asset.volatilityPercent(),
                asset.riskScore(),
                asset.riskLevel(),
                asset.dataPoints()
        );
    }
}
package com.investnavigator.backend.marketdata.provider.moex;

import com.investnavigator.backend.asset.model.Asset;
import com.investnavigator.backend.asset.model.AssetType;
import com.investnavigator.backend.common.error.BadRequestException;
import com.investnavigator.backend.common.error.ResourceNotFoundException;
import com.investnavigator.backend.marketdata.dto.CandleResponse;
import com.investnavigator.backend.marketdata.dto.MarketPriceResponse;
import com.investnavigator.backend.marketdata.model.Timeframe;
import com.investnavigator.backend.marketdata.provider.MarketDataProvider;
import com.investnavigator.backend.marketdata.provider.MarketDataProviderType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Service
public class MoexMarketDataProvider implements MarketDataProvider {

    private static final ZoneId MOEX_ZONE = ZoneId.of("Europe/Moscow");

    private static final int CANDLES_LIMIT = 30;

    private static final List<String> PRICE_COLUMNS = List.of(
            "LAST",
            "MARKETPRICE",
            "LCURRENTPRICE",
            "CLOSE",
            "PREVPRICE",
            "WAPRICE"
    );

    private static final List<String> VOLUME_COLUMNS = List.of(
            "VOLTODAY",
            "VOLUME",
            "VALUE"
    );

    private final RestClient moexRestClient;

    public MoexMarketDataProvider(
            @Qualifier("moexRestClient") RestClient moexRestClient
    ) {
        this.moexRestClient = moexRestClient;
    }

    @Override
    public MarketDataProviderType getType() {
        return MarketDataProviderType.MOEX;
    }

    @Override
    public MarketPriceResponse getLatestMarketPrice(Asset asset) {
        validateStockAsset(asset);

        String ticker = asset.getTicker().toUpperCase();

        MoexResponse response = moexRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/iss/engines/stock/markets/shares/securities/{ticker}.json")
                        .queryParam("iss.meta", "off")
                        .queryParam(
                                "marketdata.columns",
                                "SECID,LAST,MARKETPRICE,LCURRENTPRICE,PREVPRICE,WAPRICE,VOLTODAY,VOLUME,VALUE,SYSTIME"
                        )
                        .queryParam(
                                "securities.columns",
                                "SECID,SHORTNAME,PREVPRICE"
                        )
                        .build(ticker)
                )
                .retrieve()
                .body(MoexResponse.class);

        if (response == null) {
            throw new ResourceNotFoundException("MOEX response is empty for asset: " + ticker);
        }

        BigDecimal price = firstDecimal(response.marketdata(), PRICE_COLUMNS)
                .or(() -> firstDecimal(response.securities(), List.of("PREVPRICE")))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MOEX market price not found for asset: " + ticker
                ));

        BigDecimal volume = firstDecimal(response.marketdata(), VOLUME_COLUMNS)
                .orElse(BigDecimal.ZERO);

        Instant timestamp = firstText(response.marketdata(), List.of("SYSTIME", "TIME", "UPDATETIME"))
                .map(this::parseMoexTimestamp)
                .orElse(Instant.now());

        return new MarketPriceResponse(
                asset.getId(),
                asset.getTicker(),
                asset.getName(),
                price,
                volume,
                "MOEX",
                timestamp
        );
    }

    @Override
    public List<CandleResponse> getCandles(Asset asset, Timeframe timeframe) {
        validateStockAsset(asset);

        String ticker = asset.getTicker().toUpperCase();

        LocalDate till = LocalDate.now(MOEX_ZONE);
        LocalDate from = calculateCandlesFromDate(timeframe, till);

        MoexResponse response = moexRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/iss/engines/stock/markets/shares/securities/{ticker}/candles.json")
                        .queryParam("iss.meta", "off")
                        .queryParam("from", from)
                        .queryParam("till", till)
                        .queryParam("interval", toMoexInterval(timeframe))
                        .queryParam("start", 0)
                        .build(ticker)
                )
                .retrieve()
                .body(MoexResponse.class);

        if (response == null || response.candles() == null || response.candles().rows().isEmpty()) {
            throw new ResourceNotFoundException("MOEX candles not found for asset: " + ticker);
        }

        List<CandleResponse> candles = response.candles()
                .rows()
                .stream()
                .map(row -> toCandleResponse(response.candles(), row, timeframe))
                .toList();

        if (candles.size() <= CANDLES_LIMIT) {
            return candles;
        }

        return candles.subList(candles.size() - CANDLES_LIMIT, candles.size());
    }

    private CandleResponse toCandleResponse(
            MoexTableResponse candlesTable,
            List<Object> row,
            Timeframe timeframe
    ) {
        BigDecimal open = requiredDecimal(candlesTable, row, "open");
        BigDecimal high = requiredDecimal(candlesTable, row, "high");
        BigDecimal low = requiredDecimal(candlesTable, row, "low");
        BigDecimal close = requiredDecimal(candlesTable, row, "close");

        BigDecimal volume = firstDecimal(candlesTable, row, List.of("volume", "value"))
                .orElse(BigDecimal.ZERO);

        Instant timestamp = firstText(candlesTable, row, List.of("begin", "end"))
                .map(this::parseMoexTimestamp)
                .orElse(Instant.now());

        return new CandleResponse(
                timeframe,
                open,
                high,
                low,
                close,
                volume,
                "MOEX",
                timestamp
        );
    }

    private Optional<BigDecimal> firstDecimal(
            MoexTableResponse table,
            List<String> columnNames
    ) {
        if (table == null || table.rows().isEmpty()) {
            return Optional.empty();
        }

        return firstDecimal(table, table.rows().getFirst(), columnNames);
    }

    private Optional<BigDecimal> firstDecimal(
            MoexTableResponse table,
            List<Object> row,
            List<String> columnNames
    ) {
        if (table == null || row == null || columnNames == null || columnNames.isEmpty()) {
            return Optional.empty();
        }

        for (String columnName : columnNames) {
            Optional<BigDecimal> value = table.value(row, columnName)
                    .flatMap(this::toBigDecimal);

            if (value.isPresent()) {
                return value;
            }
        }

        return Optional.empty();
    }

    private BigDecimal requiredDecimal(
            MoexTableResponse table,
            List<Object> row,
            String columnName
    ) {
        return firstDecimal(table, row, List.of(columnName))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MOEX candle column is missing or empty: " + columnName
                ));
    }

    private Optional<String> firstText(
            MoexTableResponse table,
            List<String> columnNames
    ) {
        if (table == null || table.rows().isEmpty()) {
            return Optional.empty();
        }

        return firstText(table, table.rows().getFirst(), columnNames);
    }

    private Optional<String> firstText(
            MoexTableResponse table,
            List<Object> row,
            List<String> columnNames
    ) {
        if (table == null || row == null || columnNames == null || columnNames.isEmpty()) {
            return Optional.empty();
        }

        for (String columnName : columnNames) {
            Optional<String> value = table.value(row, columnName)
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(text -> !text.isBlank());

            if (value.isPresent()) {
                return value;
            }
        }

        return Optional.empty();
    }

    private Optional<BigDecimal> toBigDecimal(Object value) {
        if (value == null) {
            return Optional.empty();
        }

        try {
            String text = value.toString()
                    .trim()
                    .replace(",", ".");

            if (text.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(new BigDecimal(text));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private Instant parseMoexTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return Instant.now();
        }

        String text = value.trim();

        try {
            return Instant.parse(text);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime.parse(text.replace(" ", "T"))
                    .atZone(MOEX_ZONE)
                    .toInstant();
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDate.parse(text)
                    .atStartOfDay(MOEX_ZONE)
                    .toInstant();
        } catch (DateTimeParseException ignored) {
        }

        return Instant.now();
    }

    private LocalDate calculateCandlesFromDate(Timeframe timeframe, LocalDate till) {
        return switch (timeframe) {
            case ONE_MINUTE -> till.minusDays(3);
            case FIVE_MINUTES, FIFTEEN_MINUTES -> till.minusDays(14);
            case ONE_HOUR -> till.minusDays(45);
            case ONE_DAY -> till.minusDays(180);
        };
    }

    private int toMoexInterval(Timeframe timeframe) {
        return switch (timeframe) {
            case ONE_MINUTE -> 1;
            case FIVE_MINUTES, FIFTEEN_MINUTES -> 10;
            case ONE_HOUR -> 60;
            case ONE_DAY -> 24;
        };
    }

    private void validateStockAsset(Asset asset) {
        if (asset.getAssetType() != AssetType.STOCK) {
            throw new BadRequestException("MOEX provider supports only stock assets");
        }
    }
}
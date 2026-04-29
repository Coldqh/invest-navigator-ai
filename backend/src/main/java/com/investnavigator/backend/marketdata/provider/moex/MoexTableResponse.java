package com.investnavigator.backend.marketdata.provider.moex;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MoexTableResponse(
        @JsonAlias({"columns", "column"})
        List<String> columns,

        List<List<Object>> data
) {
    public List<List<Object>> rows() {
        if (data == null) {
            return List.of();
        }

        return data;
    }

    public Optional<Object> value(List<Object> row, String columnName) {
        if (row == null || columnName == null || columnName.isBlank()) {
            return Optional.empty();
        }

        int index = columnIndex(columnName);

        if (index < 0 || index >= row.size()) {
            return Optional.empty();
        }

        Object value = row.get(index);

        if (value == null) {
            return Optional.empty();
        }

        if (value.toString().isBlank()) {
            return Optional.empty();
        }

        return Optional.of(value);
    }

    private int columnIndex(String columnName) {
        if (columns == null || columns.isEmpty()) {
            return -1;
        }

        for (int i = 0; i < columns.size(); i++) {
            if (columnName.equalsIgnoreCase(columns.get(i))) {
                return i;
            }
        }

        return -1;
    }
}
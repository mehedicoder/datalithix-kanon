package ai.datalithix.kanon.common.model;

import java.util.List;

public record QueryFilter(
        String field,
        FilterOperator operator,
        List<String> values
) {
    public QueryFilter {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("field is required");
        }
        if (operator == null) {
            throw new IllegalArgumentException("operator is required");
        }
        values = values == null ? List.of() : List.copyOf(values);
        if (values.isEmpty()) {
            throw new IllegalArgumentException("values are required");
        }
    }
}

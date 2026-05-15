package ai.datalithix.kanon.api.support;

import ai.datalithix.kanon.common.model.PageSpec;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.common.model.SortDirection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ApiQuerySupport {
    public static QuerySpec query(
            String tenantId,
            int page,
            int size,
            String sortBy,
            SortDirection sortDirection,
            Map<String, String> dimensions
    ) {
        return new QuerySpec(
                tenantId,
                new PageSpec(page, size, sortBy, sortDirection),
                null,
                clean(dimensions)
        );
    }

    public static Map<String, String> dimensions(Object... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("dimension pairs must be key/value aligned");
        }
        Map<String, String> dimensions = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            String key = (String) pairs[i];
            Object value = pairs[i + 1];
            if (value != null && !value.toString().isBlank()) {
                dimensions.put(key, value.toString());
            }
        }
        return dimensions;
    }

    private static Map<String, String> clean(Map<String, String> dimensions) {
        if (dimensions == null || dimensions.isEmpty()) {
            return Map.of();
        }
        Map<String, String> clean = new LinkedHashMap<>();
        dimensions.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
                clean.put(key, value);
            }
        });
        return clean;
    }

    private ApiQuerySupport() {}
}

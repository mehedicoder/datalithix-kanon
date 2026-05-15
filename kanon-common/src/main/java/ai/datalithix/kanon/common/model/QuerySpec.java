package ai.datalithix.kanon.common.model;

import java.util.List;
import java.util.Map;

public record QuerySpec(
        String tenantId,
        PageSpec page,
        List<QueryFilter> filters,
        Map<String, String> dimensions
) {
    public QuerySpec {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (page == null) {
            throw new IllegalArgumentException("page is required");
        }
        filters = filters == null ? List.of() : List.copyOf(filters);
        dimensions = dimensions == null ? Map.of() : Map.copyOf(dimensions);
    }
}

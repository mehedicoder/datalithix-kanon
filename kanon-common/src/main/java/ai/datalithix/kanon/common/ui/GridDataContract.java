package ai.datalithix.kanon.common.ui;

import ai.datalithix.kanon.common.model.QuerySpec;
import java.util.List;

public record GridDataContract(
        String viewKey,
        QuerySpec initialQuery,
        List<String> requiredColumns,
        boolean lazyLoadingRequired,
        boolean aggregateBackedSummaries,
        boolean payloadPreviewOnDemand
) {
    public GridDataContract {
        if (viewKey == null || viewKey.isBlank()) {
            throw new IllegalArgumentException("viewKey is required");
        }
        if (initialQuery == null) {
            throw new IllegalArgumentException("initialQuery is required");
        }
        requiredColumns = requiredColumns == null ? List.of() : List.copyOf(requiredColumns);
        if (!lazyLoadingRequired) {
            throw new IllegalArgumentException("grid data must be lazy loaded");
        }
    }
}

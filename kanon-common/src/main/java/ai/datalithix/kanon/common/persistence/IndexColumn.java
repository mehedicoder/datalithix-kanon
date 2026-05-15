package ai.datalithix.kanon.common.persistence;

import ai.datalithix.kanon.common.model.SortDirection;

public record IndexColumn(
        String columnName,
        SortDirection sortDirection
) {
    public IndexColumn {
        if (columnName == null || columnName.isBlank()) {
            throw new IllegalArgumentException("columnName is required");
        }
        if (sortDirection == null) {
            sortDirection = SortDirection.ASC;
        }
    }
}

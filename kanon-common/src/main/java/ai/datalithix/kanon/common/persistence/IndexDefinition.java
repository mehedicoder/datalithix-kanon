package ai.datalithix.kanon.common.persistence;

import java.util.List;

public record IndexDefinition(
        String name,
        List<IndexColumn> columns,
        boolean unique,
        String accessPattern
) {
    public IndexDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        columns = columns == null ? List.of() : List.copyOf(columns);
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("columns are required");
        }
        if (accessPattern == null || accessPattern.isBlank()) {
            throw new IllegalArgumentException("accessPattern is required");
        }
    }
}

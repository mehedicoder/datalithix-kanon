package ai.datalithix.kanon.common.persistence;

import java.util.List;

public record TableContract(
        String tableName,
        boolean tenantOwned,
        boolean appendOnly,
        boolean optimisticLockingRequired,
        List<String> accessControlColumns,
        List<IndexDefinition> indexes
) {
    public TableContract {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("tableName is required");
        }
        accessControlColumns = accessControlColumns == null ? List.of() : List.copyOf(accessControlColumns);
        indexes = indexes == null ? List.of() : List.copyOf(indexes);
        if (tenantOwned && accessControlColumns.stream().noneMatch("tenant_id"::equals)) {
            throw new IllegalArgumentException("tenant-owned tables must include tenant_id");
        }
        if (tenantOwned && indexes.stream().noneMatch(TableContract::startsWithTenantId)) {
            throw new IllegalArgumentException("tenant-owned tables must include at least one tenant-first index");
        }
    }

    private static boolean startsWithTenantId(IndexDefinition index) {
        return !index.columns().isEmpty() && "tenant_id".equals(index.columns().getFirst().columnName());
    }
}

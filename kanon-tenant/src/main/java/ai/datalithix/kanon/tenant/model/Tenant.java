package ai.datalithix.kanon.tenant.model;

import ai.datalithix.kanon.common.model.AuditMetadata;

public record Tenant(
        String tenantId,
        String tenantKey,
        String name,
        GovernanceStatus status,
        String dataResidency,
        String defaultLocale,
        AuditMetadata audit
) {
    public Tenant {
        require(tenantId, "tenantId");
        require(tenantKey, "tenantKey");
        require(name, "name");
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (audit == null) {
            throw new IllegalArgumentException("audit is required");
        }
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}

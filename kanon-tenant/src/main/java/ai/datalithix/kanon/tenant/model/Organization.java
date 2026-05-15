package ai.datalithix.kanon.tenant.model;

import ai.datalithix.kanon.common.model.AuditMetadata;

public record Organization(
        String organizationId,
        String tenantId,
        String organizationKey,
        String name,
        GovernanceStatus status,
        AuditMetadata audit
) {
    public Organization {
        require(organizationId, "organizationId");
        require(tenantId, "tenantId");
        require(organizationKey, "organizationKey");
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

package ai.datalithix.kanon.tenant.model;

import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.model.AuditMetadata;

public record Workspace(
        String workspaceId,
        String tenantId,
        String organizationId,
        String workspaceKey,
        String name,
        WorkspaceType workspaceType,
        DomainType domainType,
        GovernanceStatus status,
        AuditMetadata audit
) {
    public Workspace {
        require(workspaceId, "workspaceId");
        require(tenantId, "tenantId");
        require(organizationId, "organizationId");
        require(workspaceKey, "workspaceKey");
        require(name, "name");
        if (workspaceType == null) {
            throw new IllegalArgumentException("workspaceType is required");
        }
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

package ai.datalithix.kanon.common.security;

import ai.datalithix.kanon.common.model.AuditMetadata;
import java.time.Instant;

public record PermissionGrant(
        String grantId,
        String tenantId,
        String principalId,
        SecurityPrincipalType principalType,
        Permission permission,
        String resourceType,
        String resourceId,
        Instant validFrom,
        Instant validUntil,
        AuditMetadata audit
) {
    public PermissionGrant {
        if (grantId == null || grantId.isBlank()) {
            throw new IllegalArgumentException("grantId is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (principalId == null || principalId.isBlank()) {
            throw new IllegalArgumentException("principalId is required");
        }
        if (principalType == null) {
            throw new IllegalArgumentException("principalType is required");
        }
        if (permission == null) {
            throw new IllegalArgumentException("permission is required");
        }
    }
}

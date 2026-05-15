package ai.datalithix.kanon.common.security;

import ai.datalithix.kanon.common.model.AuditMetadata;
import java.time.Instant;
import java.util.Set;

public record BreakGlassGrant(
        String grantId,
        String tenantId,
        String userId,
        String reason,
        String approvedBy,
        Instant startsAt,
        Instant expiresAt,
        Set<Permission> permissions,
        String evidenceEventId,
        AuditMetadata audit
) {
    public BreakGlassGrant {
        if (grantId == null || grantId.isBlank()) {
            throw new IllegalArgumentException("grantId is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt is required");
        }
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }
}

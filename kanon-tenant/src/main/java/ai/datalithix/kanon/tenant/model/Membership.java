package ai.datalithix.kanon.tenant.model;

import ai.datalithix.kanon.common.model.AuditMetadata;
import java.time.Instant;
import java.util.Set;

public record Membership(
        String membershipId,
        String userId,
        MembershipScope scope,
        String tenantId,
        String organizationId,
        String workspaceId,
        GovernanceStatus status,
        Instant startsAt,
        Instant expiresAt,
        Set<String> roleKeys,
        AuditMetadata audit
) {
    public Membership {
        require(membershipId, "membershipId");
        require(userId, "userId");
        if (scope == null) {
            throw new IllegalArgumentException("scope is required");
        }
        validateScope(scope, tenantId, organizationId, workspaceId);
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (audit == null) {
            throw new IllegalArgumentException("audit is required");
        }
        roleKeys = roleKeys == null ? Set.of() : Set.copyOf(roleKeys);
    }

    private static void validateScope(MembershipScope scope, String tenantId, String organizationId, String workspaceId) {
        if (scope == MembershipScope.TENANT) {
            require(tenantId, "tenantId");
        }
        if (scope == MembershipScope.ORGANIZATION) {
            require(tenantId, "tenantId");
            require(organizationId, "organizationId");
        }
        if (scope == MembershipScope.WORKSPACE) {
            require(tenantId, "tenantId");
            require(organizationId, "organizationId");
            require(workspaceId, "workspaceId");
        }
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}

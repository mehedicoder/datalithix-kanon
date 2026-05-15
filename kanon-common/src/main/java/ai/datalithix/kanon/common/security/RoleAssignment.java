package ai.datalithix.kanon.common.security;

import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import java.time.Instant;
import java.util.Set;

public record RoleAssignment(
        String assignmentId,
        String tenantId,
        String principalId,
        SecurityPrincipalType principalType,
        SecurityRole role,
        Set<DomainType> domainScope,
        Set<String> assignedCaseIds,
        Instant validFrom,
        Instant validUntil,
        AuditMetadata audit
) {
    public RoleAssignment {
        if (assignmentId == null || assignmentId.isBlank()) {
            throw new IllegalArgumentException("assignmentId is required");
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
        if (role == null) {
            throw new IllegalArgumentException("role is required");
        }
        domainScope = domainScope == null ? Set.of() : Set.copyOf(domainScope);
        assignedCaseIds = assignedCaseIds == null ? Set.of() : Set.copyOf(assignedCaseIds);
    }
}

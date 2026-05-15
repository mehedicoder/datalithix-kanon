package ai.datalithix.kanon.common.security;

import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.compliance.DataClassification;
import java.util.Set;

public record AccessControlContext(
        String tenantId,
        String userId,
        Set<SecurityRole> roles,
        Set<Permission> permissions,
        Set<DomainType> domainScope,
        Set<String> assignedCaseIds,
        Set<DataClassification> allowedClassifications,
        AccessPurpose purpose
) {
    public boolean hasPermission(Permission permission) {
        return permissions != null && permissions.contains(permission);
    }

    public boolean hasRole(SecurityRole role) {
        return roles != null && roles.contains(role);
    }
}

package ai.datalithix.kanon.tenant.model;

import ai.datalithix.kanon.common.model.AuditMetadata;
import java.util.Set;

public record Role(
        String roleId,
        String roleKey,
        String name,
        RoleScope allowedScope,
        boolean systemRole,
        Set<String> permissions,
        AuditMetadata audit
) {
    public Role {
        require(roleId, "roleId");
        require(roleKey, "roleKey");
        require(name, "name");
        if (allowedScope == null) {
            throw new IllegalArgumentException("allowedScope is required");
        }
        if (audit == null) {
            throw new IllegalArgumentException("audit is required");
        }
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}

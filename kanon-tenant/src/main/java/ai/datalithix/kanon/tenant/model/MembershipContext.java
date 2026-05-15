package ai.datalithix.kanon.tenant.model;

import java.util.Set;

public record MembershipContext(
        String membershipId,
        MembershipScope scope,
        String tenantId,
        String organizationId,
        String workspaceId,
        Set<String> roleKeys,
        Set<String> permissions
) {
    public MembershipContext {
        roleKeys = roleKeys == null ? Set.of() : Set.copyOf(roleKeys);
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }
}

package ai.datalithix.kanon.tenant.model;

import java.util.List;
import java.util.Set;

public record CurrentUserContext(
        String userId,
        String username,
        String activeTenantId,
        String activeOrganizationId,
        String activeWorkspaceId,
        Set<String> roleKeys,
        Set<String> permissions,
        List<MembershipContext> memberships
) {
    public CurrentUserContext {
        roleKeys = roleKeys == null ? Set.of() : Set.copyOf(roleKeys);
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        memberships = memberships == null ? List.of() : List.copyOf(memberships);
    }
}

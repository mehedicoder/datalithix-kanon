package ai.datalithix.kanon.policy.security;

import ai.datalithix.kanon.common.security.AccessControlContext;
import ai.datalithix.kanon.common.security.Permission;
import java.util.Map;

public interface SecurityPredicateFactory {
    Map<String, String> tenantScopedPredicates(AccessControlContext context, Permission permission, String resourceType);
}

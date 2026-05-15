package ai.datalithix.kanon.policy.security;

import ai.datalithix.kanon.common.security.AccessControlContext;
import ai.datalithix.kanon.common.security.Permission;

public interface AuthorizationService {
    AuthorizationDecision authorize(AccessControlContext context, Permission permission, ProtectedResource resource);
}

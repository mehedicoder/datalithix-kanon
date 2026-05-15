package ai.datalithix.kanon.policy.security;

import ai.datalithix.kanon.common.security.AccessControlContext;
import ai.datalithix.kanon.common.security.AccessPurpose;

public interface AccessControlContextResolver {
    AccessControlContext resolve(String tenantId, String userId, AccessPurpose purpose);
}

package ai.datalithix.kanon.policy.security;

import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.common.security.PermissionGrant;
import ai.datalithix.kanon.common.security.SecurityPrincipalType;
import java.util.List;

public interface PermissionGrantRepository {
    List<PermissionGrant> findActiveGrants(String tenantId, String principalId, SecurityPrincipalType principalType);

    PageResult<PermissionGrant> findPage(QuerySpec query);

    PermissionGrant save(PermissionGrant grant);
}

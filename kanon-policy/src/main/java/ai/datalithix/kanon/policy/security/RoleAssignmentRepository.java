package ai.datalithix.kanon.policy.security;

import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.common.security.RoleAssignment;
import ai.datalithix.kanon.common.security.SecurityPrincipalType;
import java.util.List;

public interface RoleAssignmentRepository {
    List<RoleAssignment> findActiveAssignments(String tenantId, String principalId, SecurityPrincipalType principalType);

    PageResult<RoleAssignment> findPage(QuerySpec query);

    RoleAssignment save(RoleAssignment assignment);
}

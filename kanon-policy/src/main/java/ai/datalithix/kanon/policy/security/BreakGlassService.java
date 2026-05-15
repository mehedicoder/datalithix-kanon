package ai.datalithix.kanon.policy.security;

import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.common.security.BreakGlassGrant;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public interface BreakGlassService {
    BreakGlassGrant request(String tenantId, String userId, String reason, Set<String> requestedPermissionKeys,
                            Instant expiresAt, String actorId);
    BreakGlassGrant approve(String grantId, String tenantId, String approvedBy);
    BreakGlassGrant deny(String grantId, String tenantId, String deniedBy, String reason);
    BreakGlassGrant revoke(String grantId, String tenantId, String revokedBy, String reason);
    Optional<BreakGlassGrant> findActiveGrant(String tenantId, String userId);
    Optional<BreakGlassGrant> findById(String tenantId, String grantId);
    PageResult<BreakGlassGrant> findPage(QuerySpec query);
}

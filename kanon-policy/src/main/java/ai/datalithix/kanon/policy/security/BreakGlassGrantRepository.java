package ai.datalithix.kanon.policy.security;

import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.common.security.BreakGlassGrant;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BreakGlassGrantRepository {
    Optional<BreakGlassGrant> findActiveGrant(String tenantId, String userId, Instant at);

    List<BreakGlassGrant> findExpiringBefore(Instant expiresBefore);

    PageResult<BreakGlassGrant> findPage(QuerySpec query);

    BreakGlassGrant save(BreakGlassGrant grant);
}

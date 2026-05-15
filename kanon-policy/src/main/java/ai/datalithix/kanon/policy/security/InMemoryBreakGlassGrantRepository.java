package ai.datalithix.kanon.policy.security;

import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.common.security.BreakGlassGrant;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBreakGlassGrantRepository implements BreakGlassGrantRepository {
    private final Map<String, BreakGlassGrant> grants = new ConcurrentHashMap<>();

    @Override
    public Optional<BreakGlassGrant> findActiveGrant(String tenantId, String userId, Instant at) {
        return grants.values().stream()
                .filter(g -> g.tenantId().equals(tenantId))
                .filter(g -> g.userId().equals(userId))
                .filter(g -> !g.startsAt().isAfter(at))
                .filter(g -> g.expiresAt().isAfter(at))
                .findFirst();
    }

    @Override
    public List<BreakGlassGrant> findExpiringBefore(Instant expiresBefore) {
        return grants.values().stream()
                .filter(g -> g.expiresAt().isBefore(expiresBefore))
                .sorted(Comparator.comparing(BreakGlassGrant::expiresAt))
                .toList();
    }

    @Override
    public PageResult<BreakGlassGrant> findPage(QuerySpec query) {
        var matching = grants.values().stream()
                .filter(g -> g.tenantId().equals(query.tenantId()))
                .sorted(Comparator.comparing(g -> g.audit().createdAt(), Comparator.reverseOrder()))
                .toList();
        var page = query.page();
        int from = page.pageNumber() * page.pageSize();
        int to = Math.min(from + page.pageSize(), matching.size());
        var items = from >= matching.size() ? List.<BreakGlassGrant>of() : matching.subList(from, to);
        return new PageResult<>(items, page.pageNumber(), page.pageSize(), matching.size());
    }

    @Override
    public BreakGlassGrant save(BreakGlassGrant grant) {
        grants.put(grant.grantId(), grant);
        return grant;
    }
}

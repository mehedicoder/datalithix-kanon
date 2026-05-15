package ai.datalithix.kanon.policy.security;

import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.common.security.SecurityAuditEvent;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemorySecurityAuditEventRepository implements SecurityAuditEventRepository {
    private final CopyOnWriteArrayList<SecurityAuditEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public SecurityAuditEvent save(SecurityAuditEvent event) {
        events.add(event);
        return event;
    }

    @Override
    public PageResult<SecurityAuditEvent> findPage(QuerySpec query) {
        var filtered = events.stream()
                .filter(e -> e.tenantId().equals(query.tenantId()))
                .sorted(Comparator.comparing(SecurityAuditEvent::occurredAt).reversed())
                .toList();
        var total = filtered.size();
        var offset = query.page().pageNumber() * query.page().pageSize();
        var page = filtered.stream()
                .skip(offset)
                .limit(query.page().pageSize())
                .toList();
        return new PageResult<>(page, total, query.page().pageNumber(), query.page().pageSize());
    }

    @Override
    public List<SecurityAuditEvent> findByTenant(String tenantId) {
        return events.stream()
                .filter(e -> e.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(SecurityAuditEvent::occurredAt).reversed())
                .toList();
    }

    @Override
    public List<SecurityAuditEvent> findByUser(String userId) {
        return events.stream()
                .filter(e -> userId.equals(e.actorId()))
                .sorted(Comparator.comparing(SecurityAuditEvent::occurredAt).reversed())
                .toList();
    }
}

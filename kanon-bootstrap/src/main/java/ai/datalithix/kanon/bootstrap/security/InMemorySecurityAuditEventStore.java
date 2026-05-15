package ai.datalithix.kanon.bootstrap.security;

import ai.datalithix.kanon.common.security.SecurityAuditEvent;
import ai.datalithix.kanon.policy.security.SecurityAuditEventPublisher;
import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.service.CurrentUserContextService;
import ai.datalithix.kanon.tenant.service.SecurityAuditQueryService;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;

@Service
public class InMemorySecurityAuditEventStore implements SecurityAuditEventPublisher, SecurityAuditQueryService {
    private final CopyOnWriteArrayList<SecurityAuditEvent> events = new CopyOnWriteArrayList<>();
    private final CurrentUserContextService currentUserContextService;

    public InMemorySecurityAuditEventStore(CurrentUserContextService currentUserContextService) {
        this.currentUserContextService = currentUserContextService;
    }

    @Override
    public void publish(SecurityAuditEvent event) {
        events.add(event);
    }

    @Override
    public List<SecurityAuditEvent> recent(int limit) {
        CurrentUserContext context = currentUserContextService.currentUser();
        int boundedLimit = limit <= 0 ? 50 : Math.min(limit, 500);
        return events.stream()
                .filter(event -> context.permissions().contains("platform.audit.read")
                        || context.permissions().contains("tenant.audit.read") && context.activeTenantId().equals(event.tenantId())
                        || context.permissions().contains("workspace.audit.read") && context.activeTenantId().equals(event.tenantId()))
                .sorted(Comparator.comparing(SecurityAuditEvent::occurredAt).reversed())
                .limit(boundedLimit)
                .toList();
    }
}

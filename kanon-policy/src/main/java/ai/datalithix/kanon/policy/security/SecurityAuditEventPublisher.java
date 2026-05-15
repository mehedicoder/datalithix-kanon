package ai.datalithix.kanon.policy.security;

import ai.datalithix.kanon.common.security.SecurityAuditEvent;

public interface SecurityAuditEventPublisher {
    void publish(SecurityAuditEvent event);
}

package ai.datalithix.kanon.tenant.service;

import ai.datalithix.kanon.common.security.SecurityAuditEvent;
import java.util.List;

public interface SecurityAuditQueryService {
    List<SecurityAuditEvent> recent(int limit);
}

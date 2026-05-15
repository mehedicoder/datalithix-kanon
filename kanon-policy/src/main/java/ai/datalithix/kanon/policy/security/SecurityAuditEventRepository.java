package ai.datalithix.kanon.policy.security;

import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.common.security.SecurityAuditEvent;
import java.util.List;

public interface SecurityAuditEventRepository {
    SecurityAuditEvent save(SecurityAuditEvent event);
    PageResult<SecurityAuditEvent> findPage(QuerySpec query);
    List<SecurityAuditEvent> findByTenant(String tenantId);
    List<SecurityAuditEvent> findByUser(String userId);
}

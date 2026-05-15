package ai.datalithix.kanon.policy.service;

import ai.datalithix.kanon.common.TenantContext;
import ai.datalithix.kanon.domain.model.TaskDescriptor;
import ai.datalithix.kanon.policy.model.PolicyDecision;

public interface PolicyEngine {
    PolicyDecision evaluate(TenantContext tenantContext, TaskDescriptor taskDescriptor);
}

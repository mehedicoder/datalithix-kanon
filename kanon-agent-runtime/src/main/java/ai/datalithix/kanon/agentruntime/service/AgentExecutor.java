package ai.datalithix.kanon.agentruntime.service;

import ai.datalithix.kanon.agentruntime.model.AgentExecutionResult;
import ai.datalithix.kanon.common.TenantContext;
import ai.datalithix.kanon.domain.model.TaskDescriptor;

public interface AgentExecutor {
    AgentExecutionResult execute(String agentKey, TenantContext tenantContext, TaskDescriptor taskDescriptor);
}

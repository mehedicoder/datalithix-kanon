package ai.datalithix.kanon.workflow.service;

import ai.datalithix.kanon.common.TenantContext;
import ai.datalithix.kanon.domain.model.TaskDescriptor;
import ai.datalithix.kanon.workflow.model.WorkflowPlan;

public interface WorkflowPlanner {
    WorkflowPlan plan(TenantContext tenantContext, TaskDescriptor taskDescriptor);
}

package ai.datalithix.kanon.workflow.service;

import ai.datalithix.kanon.common.service.PagedQueryPort;
import ai.datalithix.kanon.workflow.model.WorkflowDefinition;
import ai.datalithix.kanon.workflow.model.WorkflowType;
import java.util.List;
import java.util.Optional;

public interface WorkflowDefinitionRepository extends PagedQueryPort<WorkflowDefinition> {
    WorkflowDefinition save(WorkflowDefinition workflowDefinition);

    Optional<WorkflowDefinition> findById(String tenantId, String workflowId);

    void deleteById(String tenantId, String workflowId);

    List<WorkflowDefinition> findEnabledByTenant(String tenantId);

    List<WorkflowDefinition> findEnabledByType(String tenantId, WorkflowType workflowType);
}

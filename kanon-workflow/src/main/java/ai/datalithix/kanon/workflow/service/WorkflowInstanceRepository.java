package ai.datalithix.kanon.workflow.service;

import ai.datalithix.kanon.common.service.PagedQueryPort;
import ai.datalithix.kanon.workflow.model.WorkflowInstance;
import java.util.List;
import java.util.Optional;

public interface WorkflowInstanceRepository extends PagedQueryPort<WorkflowInstance> {
    WorkflowInstance save(WorkflowInstance workflowInstance);

    Optional<WorkflowInstance> findById(String tenantId, String workflowInstanceId);

    List<WorkflowInstance> findByCaseId(String tenantId, String caseId);

    List<WorkflowInstance> findOpenReviewTasks(String tenantId, String assignedUserId);
}

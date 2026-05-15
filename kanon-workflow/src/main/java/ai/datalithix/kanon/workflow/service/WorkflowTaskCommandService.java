package ai.datalithix.kanon.workflow.service;

import ai.datalithix.kanon.workflow.model.WorkflowActionRequest;
import ai.datalithix.kanon.workflow.model.WorkflowInstance;

public interface WorkflowTaskCommandService {
    WorkflowInstance startReview(WorkflowActionRequest request);

    WorkflowInstance completeReview(WorkflowActionRequest request);

    WorkflowInstance approve(WorkflowActionRequest request);

    WorkflowInstance reject(WorkflowActionRequest request);

    WorkflowInstance escalate(WorkflowActionRequest request);

    WorkflowInstance markExportReady(WorkflowActionRequest request);
}

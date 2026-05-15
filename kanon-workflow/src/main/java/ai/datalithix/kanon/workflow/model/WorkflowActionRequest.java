package ai.datalithix.kanon.workflow.model;

public record WorkflowActionRequest(
        String tenantId,
        String workflowInstanceId,
        String actorId,
        String reason
) {
    public WorkflowActionRequest {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (workflowInstanceId == null || workflowInstanceId.isBlank()) {
            throw new IllegalArgumentException("workflowInstanceId is required");
        }
        if (actorId == null || actorId.isBlank()) {
            actorId = "system";
        }
    }
}

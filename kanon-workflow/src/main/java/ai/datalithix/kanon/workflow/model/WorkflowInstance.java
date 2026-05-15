package ai.datalithix.kanon.workflow.model;

import ai.datalithix.kanon.common.model.AuditMetadata;
import java.time.Instant;
import java.util.List;

public record WorkflowInstance(
        String workflowInstanceId,
        String workflowId,
        String tenantId,
        String organizationId,
        String workspaceId,
        String caseId,
        String mediaAssetId,
        String currentStep,
        String currentState,
        String assignedAgentId,
        String assignedUserId,
        String assignedMembershipId,
        int priority,
        Instant dueAt,
        Instant startedAt,
        Instant completedAt,
        Instant failedAt,
        String failureReason,
        boolean reviewRequired,
        ReviewStatus reviewStatus,
        String reviewerId,
        String reviewerMembershipId,
        ApprovalStatus approvalStatus,
        String approvedBy,
        String approverMembershipId,
        Instant approvedAt,
        String escalationReason,
        boolean exportReady,
        List<String> evidenceEventIds,
        String traceId,
        String correlationId,
        List<String> modelInvocationIds,
        List<String> inputAssetIds,
        List<String> outputAssetIds,
        AuditMetadata audit
) {
    public WorkflowInstance(
            String workflowInstanceId,
            String workflowId,
            String tenantId,
            String caseId,
            String mediaAssetId,
            String currentStep,
            String currentState,
            String assignedAgentId,
            String assignedUserId,
            int priority,
            Instant dueAt,
            Instant startedAt,
            Instant completedAt,
            Instant failedAt,
            String failureReason,
            boolean reviewRequired,
            ReviewStatus reviewStatus,
            String reviewerId,
            ApprovalStatus approvalStatus,
            String approvedBy,
            Instant approvedAt,
            String escalationReason,
            boolean exportReady,
            List<String> evidenceEventIds,
            String traceId,
            String correlationId,
            List<String> modelInvocationIds,
            List<String> inputAssetIds,
            List<String> outputAssetIds,
            AuditMetadata audit
    ) {
        this(workflowInstanceId, workflowId, tenantId, "default-org", "administration", caseId, mediaAssetId,
                currentStep, currentState, assignedAgentId, assignedUserId, null, priority, dueAt, startedAt,
                completedAt, failedAt, failureReason, reviewRequired, reviewStatus, reviewerId, null,
                approvalStatus, approvedBy, null, approvedAt, escalationReason, exportReady, evidenceEventIds,
                traceId, correlationId, modelInvocationIds, inputAssetIds, outputAssetIds, audit);
    }

    public WorkflowInstance {
        if (workflowInstanceId == null || workflowInstanceId.isBlank()) {
            throw new IllegalArgumentException("workflowInstanceId is required");
        }
        if (workflowId == null || workflowId.isBlank()) {
            throw new IllegalArgumentException("workflowId is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (organizationId == null || organizationId.isBlank()) {
            throw new IllegalArgumentException("organizationId is required");
        }
        if (workspaceId == null || workspaceId.isBlank()) {
            throw new IllegalArgumentException("workspaceId is required");
        }
        if (reviewStatus == null) {
            throw new IllegalArgumentException("reviewStatus is required");
        }
        if (approvalStatus == null) {
            throw new IllegalArgumentException("approvalStatus is required");
        }
        if (audit == null) {
            throw new IllegalArgumentException("audit is required");
        }
        evidenceEventIds = copy(evidenceEventIds);
        modelInvocationIds = copy(modelInvocationIds);
        inputAssetIds = copy(inputAssetIds);
        outputAssetIds = copy(outputAssetIds);
    }

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}

package ai.datalithix.kanon.workflow.model;

import ai.datalithix.kanon.common.AiTaskType;
import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DataResidency;
import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import java.util.List;
import java.util.Set;

public record WorkflowDefinition(
        String workflowId,
        String tenantId,
        String organizationId,
        String workspaceId,
        String name,
        WorkflowType workflowType,
        String description,
        WorkflowStatus status,
        boolean enabled,
        DomainType domainType,
        AiTaskType taskType,
        AssetType assetType,
        SourceType sourceType,
        String policyProfile,
        String regulatoryAct,
        DataResidency dataResidency,
        String goal,
        PlannerType plannerType,
        String plannerVersion,
        String actionSetRef,
        List<String> preconditions,
        List<String> constraints,
        String fallbackWorkflowRef,
        String modelRoutePolicy,
        Set<String> allowedModelProfileIds,
        AuditMetadata audit
) {
    public WorkflowDefinition(
            String workflowId,
            String tenantId,
            String name,
            WorkflowType workflowType,
            String description,
            WorkflowStatus status,
            boolean enabled,
            DomainType domainType,
            AiTaskType taskType,
            AssetType assetType,
            SourceType sourceType,
            String policyProfile,
            String regulatoryAct,
            DataResidency dataResidency,
            String goal,
            PlannerType plannerType,
            String plannerVersion,
            String actionSetRef,
            List<String> preconditions,
            List<String> constraints,
            String fallbackWorkflowRef,
            String modelRoutePolicy,
            Set<String> allowedModelProfileIds,
            AuditMetadata audit
    ) {
        this(workflowId, tenantId, "default-org", "administration", name, workflowType, description, status, enabled,
                domainType, taskType, assetType, sourceType, policyProfile, regulatoryAct, dataResidency, goal,
                plannerType, plannerVersion, actionSetRef, preconditions, constraints, fallbackWorkflowRef,
                modelRoutePolicy, allowedModelProfileIds, audit);
    }

    public WorkflowDefinition {
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
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (workflowType == null) {
            throw new IllegalArgumentException("workflowType is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (plannerType == null) {
            throw new IllegalArgumentException("plannerType is required");
        }
        if (audit == null) {
            throw new IllegalArgumentException("audit is required");
        }
        preconditions = preconditions == null ? List.of() : List.copyOf(preconditions);
        constraints = constraints == null ? List.of() : List.copyOf(constraints);
        allowedModelProfileIds = allowedModelProfileIds == null ? Set.of() : Set.copyOf(allowedModelProfileIds);
    }
}

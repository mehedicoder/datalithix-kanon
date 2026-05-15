package ai.datalithix.kanon.workflow.model;

import ai.datalithix.kanon.common.AiTaskType;
import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.SourceCategory;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.common.security.AccessControlContext;
import ai.datalithix.kanon.common.security.Permission;
import ai.datalithix.kanon.policy.model.PolicyDecision;
import java.util.Map;
import java.util.Set;

public record WorkflowPlanningContext(
        String tenantId,
        String caseId,
        WorkflowType workflowType,
        DomainType domainType,
        AiTaskType taskType,
        AssetType assetType,
        SourceCategory sourceCategory,
        SourceType sourceType,
        String sourceTraceId,
        String workflowDefinitionId,
        PolicyDecision policyDecision,
        AccessControlContext accessControlContext,
        Map<String, String> attributes
) {
    public WorkflowPlanningContext {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("caseId is required");
        }
        if (workflowType == null) {
            throw new IllegalArgumentException("workflowType is required");
        }
        if (domainType == null) {
            throw new IllegalArgumentException("domainType is required");
        }
        if (taskType == null) {
            throw new IllegalArgumentException("taskType is required");
        }
        if (policyDecision == null) {
            throw new IllegalArgumentException("policyDecision is required");
        }
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public boolean hasPermissions(Set<Permission> requiredPermissions) {
        Set<Permission> permissions = requiredPermissions == null ? Set.of() : requiredPermissions;
        return permissions.isEmpty()
                || accessControlContext != null && permissions.stream().allMatch(accessControlContext::hasPermission);
    }

    public boolean policyAllows(Set<String> requiredPolicies) {
        Set<String> policies = requiredPolicies == null ? Set.of() : requiredPolicies;
        return policyDecision.allowed()
                && (policies.isEmpty() || policyDecision.activePolicies().containsAll(policies));
    }
}

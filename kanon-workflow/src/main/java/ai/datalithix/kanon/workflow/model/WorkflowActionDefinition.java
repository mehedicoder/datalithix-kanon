package ai.datalithix.kanon.workflow.model;

import ai.datalithix.kanon.common.AiTaskType;
import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.SourceCategory;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.common.security.Permission;
import java.util.Set;

public record WorkflowActionDefinition(
        String actionId,
        String name,
        String description,
        WorkflowType workflowType,
        Set<DomainType> domainTypes,
        Set<AiTaskType> taskTypes,
        Set<AssetType> assetTypes,
        Set<SourceCategory> sourceCategories,
        Set<SourceType> sourceTypes,
        Set<String> preconditions,
        Set<String> effects,
        Set<String> policyConstraints,
        Set<Permission> requiredPermissions,
        int cost,
        boolean idempotent,
        String agentTypeRef,
        String modelRoutePolicyRef
) {
    public WorkflowActionDefinition {
        if (actionId == null || actionId.isBlank()) {
            throw new IllegalArgumentException("actionId is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (workflowType == null) {
            throw new IllegalArgumentException("workflowType is required");
        }
        if (cost < 0) {
            throw new IllegalArgumentException("cost must not be negative");
        }
        domainTypes = domainTypes == null ? Set.of() : Set.copyOf(domainTypes);
        taskTypes = taskTypes == null ? Set.of() : Set.copyOf(taskTypes);
        assetTypes = assetTypes == null ? Set.of() : Set.copyOf(assetTypes);
        sourceCategories = sourceCategories == null ? Set.of() : Set.copyOf(sourceCategories);
        sourceTypes = sourceTypes == null ? Set.of() : Set.copyOf(sourceTypes);
        preconditions = preconditions == null ? Set.of() : Set.copyOf(preconditions);
        effects = effects == null ? Set.of() : Set.copyOf(effects);
        if (effects.isEmpty()) {
            throw new IllegalArgumentException("effects are required");
        }
        policyConstraints = policyConstraints == null ? Set.of() : Set.copyOf(policyConstraints);
        requiredPermissions = requiredPermissions == null ? Set.of() : Set.copyOf(requiredPermissions);
    }

    public boolean isApplicableTo(WorkflowPlanningContext context, Set<String> currentFacts) {
        if (context == null) {
            return false;
        }
        Set<String> facts = currentFacts == null ? Set.of() : currentFacts;
        return workflowType == context.workflowType()
                && matches(domainTypes, context.domainType())
                && matches(taskTypes, context.taskType())
                && matches(assetTypes, context.assetType())
                && matches(sourceCategories, context.sourceCategory())
                && matches(sourceTypes, context.sourceType())
                && facts.containsAll(preconditions)
                && context.hasPermissions(requiredPermissions)
                && context.policyAllows(policyConstraints);
    }

    private static <T> boolean matches(Set<T> allowedValues, T actualValue) {
        return allowedValues.isEmpty() || allowedValues.contains(actualValue);
    }
}

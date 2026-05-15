package ai.datalithix.kanon.workflow.model;

import java.util.Set;

public record WorkflowGoal(
        String goalId,
        String name,
        WorkflowType workflowType,
        Set<String> desiredFacts,
        Set<String> forbiddenFacts,
        Set<String> policyConstraints,
        int priority
) {
    public WorkflowGoal {
        if (goalId == null || goalId.isBlank()) {
            throw new IllegalArgumentException("goalId is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (workflowType == null) {
            throw new IllegalArgumentException("workflowType is required");
        }
        if (priority < 0) {
            throw new IllegalArgumentException("priority must not be negative");
        }
        desiredFacts = desiredFacts == null ? Set.of() : Set.copyOf(desiredFacts);
        if (desiredFacts.isEmpty()) {
            throw new IllegalArgumentException("desiredFacts are required");
        }
        forbiddenFacts = forbiddenFacts == null ? Set.of() : Set.copyOf(forbiddenFacts);
        policyConstraints = policyConstraints == null ? Set.of() : Set.copyOf(policyConstraints);
    }

    public boolean isSatisfiedBy(Set<String> facts) {
        Set<String> currentFacts = facts == null ? Set.of() : facts;
        return currentFacts.containsAll(desiredFacts)
                && forbiddenFacts.stream().noneMatch(currentFacts::contains);
    }
}

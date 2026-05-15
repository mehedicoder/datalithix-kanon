package ai.datalithix.kanon.workflow.model;

import java.util.List;
import java.util.Set;

public record WorkflowPlanningProblem(
        String planningProblemId,
        WorkflowPlanningContext context,
        WorkflowGoal goal,
        Set<String> initialFacts,
        List<WorkflowActionDefinition> availableActions
) {
    public WorkflowPlanningProblem {
        if (planningProblemId == null || planningProblemId.isBlank()) {
            throw new IllegalArgumentException("planningProblemId is required");
        }
        if (context == null) {
            throw new IllegalArgumentException("context is required");
        }
        if (goal == null) {
            throw new IllegalArgumentException("goal is required");
        }
        if (context.workflowType() != goal.workflowType()) {
            throw new IllegalArgumentException("context and goal workflowType must match");
        }
        initialFacts = initialFacts == null ? Set.of() : Set.copyOf(initialFacts);
        availableActions = availableActions == null ? List.of() : List.copyOf(availableActions);
    }

    public List<WorkflowActionDefinition> initiallyApplicableActions() {
        return availableActions.stream()
                .filter(action -> action.isApplicableTo(context, initialFacts))
                .toList();
    }
}

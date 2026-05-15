package ai.datalithix.kanon.workflow.service;

import java.util.List;

public record EmbabelPlanningResult(
        boolean planned,
        boolean goalSatisfied,
        List<String> actionIds,
        String rationale,
        String failureReason
) {
    public EmbabelPlanningResult {
        actionIds = actionIds == null ? List.of() : List.copyOf(actionIds);
    }

    public boolean usable() {
        return planned && goalSatisfied && !actionIds.isEmpty();
    }
}

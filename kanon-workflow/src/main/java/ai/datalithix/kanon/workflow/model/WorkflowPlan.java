package ai.datalithix.kanon.workflow.model;

import java.util.List;

public record WorkflowPlan(
        String workflowKey,
        String caseId,
        String correlationId,
        List<String> plannedSteps,
        WorkflowExecutionPolicy executionPolicy,
        String rationale
) {}

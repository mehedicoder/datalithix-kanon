package ai.datalithix.kanon.workflow.service;

import ai.datalithix.kanon.workflow.model.WorkflowPlanningProblem;

public interface EmbabelPlanningClient {
    EmbabelPlanningResult plan(WorkflowPlanningProblem problem);
}

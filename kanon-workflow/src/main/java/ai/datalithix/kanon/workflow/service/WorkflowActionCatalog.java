package ai.datalithix.kanon.workflow.service;

import ai.datalithix.kanon.workflow.model.WorkflowActionDefinition;
import ai.datalithix.kanon.workflow.model.WorkflowPlanningContext;
import java.util.List;

public interface WorkflowActionCatalog {
    List<WorkflowActionDefinition> actionsFor(WorkflowPlanningContext context);
}

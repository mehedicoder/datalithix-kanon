package ai.datalithix.kanon.config.model;

import ai.datalithix.kanon.common.AiTaskType;
import ai.datalithix.kanon.common.DomainType;
import java.util.List;

public record WorkflowTemplate(
        String id,
        String displayName,
        DomainType domainType,
        AiTaskType taskType,
        List<String> steps,
        String plannerType,
        String fallbackWorkflowId
) {}

package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.AnnotationExecutionModeDecision;
import ai.datalithix.kanon.annotation.model.AnnotationExecutionModePolicyInput;

public interface AnnotationExecutionModePolicy {
    AnnotationExecutionModeDecision decide(AnnotationExecutionModePolicyInput input);
}

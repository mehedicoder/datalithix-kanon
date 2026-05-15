package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.AnnotationNodeDescriptor;
import ai.datalithix.kanon.annotation.model.AnnotationResult;
import ai.datalithix.kanon.annotation.model.AnnotationTask;
import ai.datalithix.kanon.annotation.model.AnnotationTaskCreation;

public interface AnnotationNode {
    AnnotationNodeDescriptor descriptor();

    boolean supports(AnnotationTask task);

    AnnotationTaskCreation createTask(AnnotationTask task);

    AnnotationResult fetchResult(String externalTaskId);
}

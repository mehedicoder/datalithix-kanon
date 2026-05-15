package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.AnnotationResult;
import ai.datalithix.kanon.annotation.model.AnnotationSyncRecord;
import ai.datalithix.kanon.annotation.model.AnnotationTask;
import ai.datalithix.kanon.annotation.model.AnnotationTaskCreation;

public interface AnnotationSyncService {
    AnnotationTaskCreation pushTask(AnnotationTask task);

    AnnotationResult fetchResult(String nodeId, String externalTaskId);

    AnnotationResult syncResult(String nodeId, String externalTaskId);

    AnnotationSyncRecord syncRecord(String nodeId, String externalTaskId);
}

package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.AnnotationSyncRecord;
import ai.datalithix.kanon.annotation.model.AnnotationTaskStatus;
import ai.datalithix.kanon.common.service.PagedQueryPort;
import java.util.List;
import java.util.Optional;

public interface AnnotationSyncRecordRepository extends PagedQueryPort<AnnotationSyncRecord> {
    String save(String tenantId, AnnotationSyncRecord record);

    Optional<AnnotationSyncRecord> findById(String tenantId, String syncId);

    List<AnnotationSyncRecord> findByAnnotationTaskId(String tenantId, String annotationTaskId);

    List<AnnotationSyncRecord> findByNodeId(String tenantId, String nodeId);

    List<AnnotationSyncRecord> findByStatus(String tenantId, AnnotationTaskStatus status);
}

package ai.datalithix.kanon.annotation.model;

import java.time.Instant;
import java.util.Map;

public record AnnotationSyncRecord(
        String annotationTaskId,
        String nodeId,
        AnnotationExecutionNodeType nodeType,
        String externalTaskId,
        AnnotationTaskStatus status,
        String externalUrl,
        String failureReason,
        Map<String, String> metadata,
        Instant syncedAt
) {
    public AnnotationSyncRecord {
        if (annotationTaskId == null || annotationTaskId.isBlank()) {
            throw new IllegalArgumentException("annotationTaskId is required");
        }
        if (nodeId == null || nodeId.isBlank()) {
            throw new IllegalArgumentException("nodeId is required");
        }
        if (nodeType == null) {
            throw new IllegalArgumentException("nodeType is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        if (syncedAt == null) {
            syncedAt = Instant.now();
        }
    }
}

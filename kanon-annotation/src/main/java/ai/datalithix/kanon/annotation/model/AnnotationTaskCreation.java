package ai.datalithix.kanon.annotation.model;

import java.time.Instant;
import java.util.Map;

public record AnnotationTaskCreation(
        String annotationTaskId,
        String nodeId,
        AnnotationExecutionNodeType nodeType,
        String externalTaskId,
        AnnotationTaskStatus status,
        String externalUrl,
        Map<String, String> metadata,
        Instant createdAt
) {
    public AnnotationTaskCreation {
        if (annotationTaskId == null || annotationTaskId.isBlank()) {
            throw new IllegalArgumentException("annotationTaskId is required");
        }
        if (nodeId == null || nodeId.isBlank()) {
            throw new IllegalArgumentException("nodeId is required");
        }
        if (nodeType == null) {
            throw new IllegalArgumentException("nodeType is required");
        }
        if (externalTaskId == null || externalTaskId.isBlank()) {
            throw new IllegalArgumentException("externalTaskId is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

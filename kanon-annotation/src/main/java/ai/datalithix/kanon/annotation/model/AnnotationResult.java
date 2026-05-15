package ai.datalithix.kanon.annotation.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AnnotationResult(
        String annotationTaskId,
        String nodeId,
        AnnotationExecutionNodeType nodeType,
        String externalTaskId,
        AnnotationTaskStatus status,
        List<AnnotationResultItem> items,
        String rawResultRef,
        String failureReason,
        Map<String, String> metadata,
        Instant completedAt
) {
    public AnnotationResult {
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
        items = items == null ? List.of() : List.copyOf(items);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}

package ai.datalithix.kanon.annotation.service.cvat;

import ai.datalithix.kanon.annotation.model.AnnotationResultItem;
import ai.datalithix.kanon.annotation.model.AnnotationTaskStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CvatTaskResult(
        String externalTaskId,
        AnnotationTaskStatus status,
        List<AnnotationResultItem> items,
        String rawResultRef,
        String failureReason,
        Map<String, String> metadata,
        Instant completedAt
) {
    public CvatTaskResult {
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

package ai.datalithix.kanon.annotation.model;

import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DomainType;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

public record AnnotationTask(
        String annotationTaskId,
        String tenantId,
        String caseId,
        String workflowInstanceId,
        String sourceTraceId,
        String mediaAssetId,
        AssetType assetType,
        DomainType domainType,
        AnnotationExecutionNodeType preferredNodeType,
        Set<String> labels,
        Map<String, String> payloadRefs,
        Map<String, String> instructions,
        String evidenceEventId,
        Instant createdAt
) {
    public AnnotationTask {
        if (annotationTaskId == null || annotationTaskId.isBlank()) {
            throw new IllegalArgumentException("annotationTaskId is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (assetType == null) {
            throw new IllegalArgumentException("assetType is required");
        }
        labels = labels == null ? Set.of() : Set.copyOf(labels);
        payloadRefs = payloadRefs == null ? Map.of() : Map.copyOf(payloadRefs);
        instructions = instructions == null ? Map.of() : Map.copyOf(instructions);
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

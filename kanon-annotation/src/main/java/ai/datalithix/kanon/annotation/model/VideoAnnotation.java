package ai.datalithix.kanon.annotation.model;

import ai.datalithix.kanon.common.model.AuditMetadata;
import java.util.Map;

public record VideoAnnotation(
        String annotationId,
        String tenantId,
        String caseId,
        String mediaAssetId,
        Integer frameStart,
        Integer frameEnd,
        Long startTimeMs,
        Long endTimeMs,
        AnnotationGeometryType geometryType,
        String geometryJson,
        String label,
        String trackId,
        String telemetryRef,
        String reviewStatus,
        String modelInvocationId,
        String evidenceEventId,
        Map<String, String> attributes,
        AuditMetadata audit
) {
    public VideoAnnotation {
        if (annotationId == null || annotationId.isBlank()) {
            throw new IllegalArgumentException("annotationId is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (mediaAssetId == null || mediaAssetId.isBlank()) {
            throw new IllegalArgumentException("mediaAssetId is required");
        }
        if (geometryType == null) {
            throw new IllegalArgumentException("geometryType is required");
        }
        if (audit == null) {
            throw new IllegalArgumentException("audit is required");
        }
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}

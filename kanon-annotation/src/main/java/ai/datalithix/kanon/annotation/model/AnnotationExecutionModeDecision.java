package ai.datalithix.kanon.annotation.model;

import java.util.Map;

public record AnnotationExecutionModeDecision(
        AnnotationExecutionMode mode,
        boolean externalTaskRequired,
        AnnotationExecutionNodeType preferredNodeType,
        String rationale,
        Map<String, String> evidenceMetadata
) {
    public AnnotationExecutionModeDecision {
        if (mode == null) {
            throw new IllegalArgumentException("mode is required");
        }
        if (rationale == null || rationale.isBlank()) {
            throw new IllegalArgumentException("rationale is required");
        }
        evidenceMetadata = evidenceMetadata == null ? Map.of() : Map.copyOf(evidenceMetadata);
    }
}

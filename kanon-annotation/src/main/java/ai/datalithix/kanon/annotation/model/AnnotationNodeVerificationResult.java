package ai.datalithix.kanon.annotation.model;

import java.time.Instant;
import java.util.List;

public record AnnotationNodeVerificationResult(
        String nodeId,
        ExternalAnnotationNodeStatus resultingStatus,
        List<AnnotationNodeVerificationStep> steps,
        String detectedVersion,
        long totalLatencyMs,
        Instant verifiedAt
) {
    public AnnotationNodeVerificationResult {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}

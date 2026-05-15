package ai.datalithix.kanon.airouting.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public record ModelInvocationResult(
        String invocationId,
        String tenantId,
        String profileKey,
        ModelInvocationStatus status,
        String outputRef,
        String summary,
        String failureReason,
        Duration latency,
        String evidenceEventId,
        Map<String, String> metadata,
        Instant completedAt
) {
    public ModelInvocationResult {
        if (invocationId == null || invocationId.isBlank()) {
            throw new IllegalArgumentException("invocationId is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (profileKey == null || profileKey.isBlank()) {
            throw new IllegalArgumentException("profileKey is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}

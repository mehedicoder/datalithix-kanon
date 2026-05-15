package ai.datalithix.kanon.airouting.model;

import ai.datalithix.kanon.common.AiTaskType;
import ai.datalithix.kanon.common.security.AccessControlContext;
import java.time.Instant;
import java.util.Map;

public record ModelInvocationRequest(
        String invocationId,
        String tenantId,
        String caseId,
        String profileKey,
        AiTaskType taskType,
        String promptRef,
        String payloadRef,
        Map<String, String> parameters,
        String correlationId,
        Instant requestedAt,
        AccessControlContext accessContext
) {
    public ModelInvocationRequest {
        if (invocationId == null || invocationId.isBlank()) {
            throw new IllegalArgumentException("invocationId is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (profileKey == null || profileKey.isBlank()) {
            throw new IllegalArgumentException("profileKey is required");
        }
        if (taskType == null) {
            throw new IllegalArgumentException("taskType is required");
        }
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}

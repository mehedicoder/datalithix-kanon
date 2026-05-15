package ai.datalithix.kanon.training.model;

import java.util.Map;

public record ComputeBackend(
        String backendId,
        String tenantId,
        ComputeBackendType backendType,
        String name,
        String endpointUrl,
        String credentialRef,
        Map<String, String> configuration,
        boolean enabled,
        boolean healthy,
        String lastHealthCheckAt,
        String failureReason
) {
    public ComputeBackend {
        if (backendId == null || backendId.isBlank()) {
            throw new IllegalArgumentException("backendId is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (backendType == null) {
            throw new IllegalArgumentException("backendType is required");
        }
        configuration = configuration == null ? Map.of() : Map.copyOf(configuration);
    }
}

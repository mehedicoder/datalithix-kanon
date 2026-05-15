package ai.datalithix.kanon.airouting.model;

import ai.datalithix.kanon.common.AiTaskType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import java.util.Set;

public record ModelProfile(
        String profileKey,
        String provider,
        String backendType,
        String modelId,
        String modelName,
        String baseUrl,
        boolean local,
        boolean supportsTools,
        boolean supportsStructuredOutput,
        Set<AiTaskType> taskCapabilities,
        String costClass,
        String latencyClass,
        String locality,
        Set<String> complianceTags,
        boolean enabled,
        String healthStatus,
        String secretRef,
        int priority,
        ModelExecutionPolicy executionPolicy,
        AuditMetadata audit
) {
    public ModelProfile {
        if (profileKey == null || profileKey.isBlank()) {
            throw new IllegalArgumentException("profileKey is required");
        }
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("provider is required");
        }
        if (backendType == null || backendType.isBlank()) {
            throw new IllegalArgumentException("backendType is required");
        }
        if (modelId == null || modelId.isBlank()) {
            throw new IllegalArgumentException("modelId is required");
        }
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException("modelName is required");
        }
        if (executionPolicy == null) {
            throw new IllegalArgumentException("executionPolicy is required");
        }
        taskCapabilities = taskCapabilities == null ? Set.of() : Set.copyOf(taskCapabilities);
        complianceTags = complianceTags == null ? Set.of() : Set.copyOf(complianceTags);
    }
}

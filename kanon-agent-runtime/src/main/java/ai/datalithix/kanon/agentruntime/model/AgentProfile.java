package ai.datalithix.kanon.agentruntime.model;

import ai.datalithix.kanon.common.AiTaskType;
import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.common.runtime.RetryPolicy;
import java.time.Instant;
import java.util.Set;

public record AgentProfile(
        String agentId,
        String tenantId,
        String name,
        AgentType agentType,
        String description,
        AgentStatus status,
        boolean enabled,
        Set<DomainType> supportedDomains,
        Set<AiTaskType> supportedTaskTypes,
        Set<AssetType> supportedAssetTypes,
        Set<SourceType> supportedSourceTypes,
        Set<String> supportedAnnotationTypes,
        Set<String> requiredPolicies,
        Set<String> requiredPermissions,
        String inputSchemaRef,
        String outputSchemaRef,
        AgentExecutionMode executionMode,
        int timeoutSeconds,
        RetryPolicy retryPolicy,
        int maxAttempts,
        int concurrencyLimit,
        int priority,
        String queueName,
        String runtimeProfile,
        String configurationRef,
        String modelRoutePolicy,
        String preferredModelProfile,
        String fallbackModelProfile,
        Set<String> allowedModelProfileIds,
        Set<String> requiredLlmServiceCapabilities,
        boolean allowCloudModels,
        boolean allowLocalModels,
        String maxCostClass,
        String maxLatencyClass,
        Set<String> complianceTags,
        boolean evidenceRequired,
        boolean traceEnabled,
        Instant lastRunAt,
        Instant lastSuccessAt,
        Instant lastFailureAt,
        String lastFailureReason,
        AuditMetadata audit
) {
    public AgentProfile {
        if (agentId == null || agentId.isBlank()) {
            throw new IllegalArgumentException("agentId is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (agentType == null) {
            throw new IllegalArgumentException("agentType is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (executionMode == null) {
            throw new IllegalArgumentException("executionMode is required");
        }
        if (timeoutSeconds < 0) {
            throw new IllegalArgumentException("timeoutSeconds must be zero or greater");
        }
        if (maxAttempts < 0) {
            throw new IllegalArgumentException("maxAttempts must be zero or greater");
        }
        if (concurrencyLimit < 0) {
            throw new IllegalArgumentException("concurrencyLimit must be zero or greater");
        }
        if (audit == null) {
            throw new IllegalArgumentException("audit is required");
        }
        supportedDomains = copy(supportedDomains);
        supportedTaskTypes = copy(supportedTaskTypes);
        supportedAssetTypes = copy(supportedAssetTypes);
        supportedSourceTypes = copy(supportedSourceTypes);
        supportedAnnotationTypes = copy(supportedAnnotationTypes);
        requiredPolicies = copy(requiredPolicies);
        requiredPermissions = copy(requiredPermissions);
        allowedModelProfileIds = copy(allowedModelProfileIds);
        requiredLlmServiceCapabilities = copy(requiredLlmServiceCapabilities);
        complianceTags = copy(complianceTags);
    }

    private static <T> Set<T> copy(Set<T> values) {
        return values == null ? Set.of() : Set.copyOf(values);
    }
}

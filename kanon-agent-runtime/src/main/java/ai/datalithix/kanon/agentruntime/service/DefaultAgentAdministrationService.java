package ai.datalithix.kanon.agentruntime.service;

import ai.datalithix.kanon.agentruntime.model.AgentExecutionMode;
import ai.datalithix.kanon.agentruntime.model.AgentProfile;
import ai.datalithix.kanon.agentruntime.model.AgentStatus;
import ai.datalithix.kanon.agentruntime.model.AgentType;
import ai.datalithix.kanon.common.AiTaskType;
import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.common.runtime.RetryPolicy;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class DefaultAgentAdministrationService implements AgentAdministrationService {
    private final AgentProfileRepository agentProfileRepository;

    public DefaultAgentAdministrationService(AgentProfileRepository agentProfileRepository) {
        this.agentProfileRepository = agentProfileRepository;
    }

    @Override
    public PageResult<AgentProfile> findPage(QuerySpec query) {
        return agentProfileRepository.findPage(query);
    }

    @Override
    public Optional<AgentProfile> findById(String tenantId, String agentId) {
        return agentProfileRepository.findById(tenantId, agentId);
    }

    @Override
    public AgentProfile createAgent(
            String tenantId,
            String name,
            AgentType agentType,
            String description,
            Set<DomainType> supportedDomains,
            Set<AiTaskType> supportedTaskTypes,
            Set<AssetType> supportedAssetTypes,
            Set<SourceType> supportedSourceTypes,
            Set<String> supportedAnnotationTypes,
            String inputSchemaRef,
            String outputSchemaRef,
            AgentExecutionMode executionMode,
            int timeoutSeconds,
            RetryPolicy retryPolicy,
            int maxAttempts,
            int concurrencyLimit,
            int priority,
            String queueName,
            String modelRoutePolicy,
            String fallbackModelProfile,
            String actorId
    ) {
        String agentId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        AgentProfile profile = new AgentProfile(
                agentId,
                tenantId,
                name,
                agentType,
                description,
                AgentStatus.DRAFT,
                false,
                supportedDomains,
                supportedTaskTypes,
                supportedAssetTypes,
                supportedSourceTypes,
                supportedAnnotationTypes,
                Set.of(),
                Set.of(),
                inputSchemaRef,
                outputSchemaRef,
                executionMode,
                timeoutSeconds,
                retryPolicy,
                maxAttempts,
                concurrencyLimit,
                priority,
                queueName,
                null,
                null,
                modelRoutePolicy,
                null,
                fallbackModelProfile,
                Set.of(),
                Set.of(),
                false,
                false,
                null,
                null,
                Set.of(),
                false,
                false,
                null,
                null,
                null,
                null,
                new AuditMetadata(now, actorId, now, actorId, 0L)
        );
        return agentProfileRepository.save(profile);
    }

    @Override
    public AgentProfile updateAgent(
            String tenantId,
            String agentId,
            String name,
            String description,
            Set<DomainType> supportedDomains,
            Set<AiTaskType> supportedTaskTypes,
            Set<AssetType> supportedAssetTypes,
            Set<SourceType> supportedSourceTypes,
            Set<String> supportedAnnotationTypes,
            String inputSchemaRef,
            String outputSchemaRef,
            AgentExecutionMode executionMode,
            int timeoutSeconds,
            RetryPolicy retryPolicy,
            int maxAttempts,
            int concurrencyLimit,
            int priority,
            String queueName,
            String modelRoutePolicy,
            String fallbackModelProfile,
            String actorId
    ) {
        AgentProfile existing = agentProfileRepository.findById(tenantId, agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found"));
        AgentProfile updated = new AgentProfile(
                existing.agentId(),
                existing.tenantId(),
                name,
                existing.agentType(),
                description,
                existing.status(),
                existing.enabled(),
                supportedDomains,
                supportedTaskTypes,
                supportedAssetTypes,
                supportedSourceTypes,
                supportedAnnotationTypes,
                existing.requiredPolicies(),
                existing.requiredPermissions(),
                inputSchemaRef,
                outputSchemaRef,
                executionMode,
                timeoutSeconds,
                retryPolicy,
                maxAttempts,
                concurrencyLimit,
                priority,
                queueName,
                existing.runtimeProfile(),
                existing.configurationRef(),
                modelRoutePolicy,
                existing.preferredModelProfile(),
                fallbackModelProfile,
                existing.allowedModelProfileIds(),
                existing.requiredLlmServiceCapabilities(),
                existing.allowCloudModels(),
                existing.allowLocalModels(),
                existing.maxCostClass(),
                existing.maxLatencyClass(),
                existing.complianceTags(),
                existing.evidenceRequired(),
                existing.traceEnabled(),
                existing.lastRunAt(),
                existing.lastSuccessAt(),
                existing.lastFailureAt(),
                existing.lastFailureReason(),
                new AuditMetadata(existing.audit().createdAt(), existing.audit().createdBy(), Instant.now(), actorId, existing.audit().version() + 1)
        );
        return agentProfileRepository.save(updated);
    }

    @Override
    public AgentProfile enableAgent(String tenantId, String agentId, String actorId) {
        return toggleAgentStatus(tenantId, agentId, true, AgentStatus.ACTIVE, actorId);
    }

    @Override
    public AgentProfile disableAgent(String tenantId, String agentId, String actorId) {
        return toggleAgentStatus(tenantId, agentId, false, AgentStatus.DISABLED, actorId);
    }

    @Override
    public void archiveAgent(String tenantId, String agentId, String actorId) {
        toggleAgentStatus(tenantId, agentId, false, AgentStatus.RETIRED, actorId);
    }

    @Override
    public void restoreAgent(String tenantId, String agentId, String actorId) {
        toggleAgentStatus(tenantId, agentId, false, AgentStatus.DRAFT, actorId);
    }

    @Override
    public void deleteAgent(String tenantId, String agentId) {
        AgentProfile existing = agentProfileRepository.findById(tenantId, agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found"));
        if (existing.status() != AgentStatus.RETIRED) {
            throw new IllegalStateException("Only archived agents can be deleted");
        }
        agentProfileRepository.deleteById(tenantId, agentId);
    }

    private AgentProfile toggleAgentStatus(String tenantId, String agentId, boolean enabled, AgentStatus status, String actorId) {
        AgentProfile existing = agentProfileRepository.findById(tenantId, agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found"));
        AgentProfile updated = new AgentProfile(
                existing.agentId(),
                existing.tenantId(),
                existing.name(),
                existing.agentType(),
                existing.description(),
                status,
                enabled,
                existing.supportedDomains(),
                existing.supportedTaskTypes(),
                existing.supportedAssetTypes(),
                existing.supportedSourceTypes(),
                existing.supportedAnnotationTypes(),
                existing.requiredPolicies(),
                existing.requiredPermissions(),
                existing.inputSchemaRef(),
                existing.outputSchemaRef(),
                existing.executionMode(),
                existing.timeoutSeconds(),
                existing.retryPolicy(),
                existing.maxAttempts(),
                existing.concurrencyLimit(),
                existing.priority(),
                existing.queueName(),
                existing.runtimeProfile(),
                existing.configurationRef(),
                existing.modelRoutePolicy(),
                existing.preferredModelProfile(),
                existing.fallbackModelProfile(),
                existing.allowedModelProfileIds(),
                existing.requiredLlmServiceCapabilities(),
                existing.allowCloudModels(),
                existing.allowLocalModels(),
                existing.maxCostClass(),
                existing.maxLatencyClass(),
                existing.complianceTags(),
                existing.evidenceRequired(),
                existing.traceEnabled(),
                existing.lastRunAt(),
                existing.lastSuccessAt(),
                existing.lastFailureAt(),
                existing.lastFailureReason(),
                new AuditMetadata(existing.audit().createdAt(), existing.audit().createdBy(), Instant.now(), actorId, existing.audit().version() + 1)
        );
        return agentProfileRepository.save(updated);
    }
}

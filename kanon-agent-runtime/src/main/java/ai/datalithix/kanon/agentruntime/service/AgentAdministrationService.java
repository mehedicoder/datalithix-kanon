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

public interface AgentAdministrationService {
    PageResult<AgentProfile> findPage(QuerySpec query);

    Optional<AgentProfile> findById(String tenantId, String agentId);

    AgentProfile createAgent(
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
    );

    AgentProfile updateAgent(
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
    );

    AgentProfile enableAgent(String tenantId, String agentId, String actorId);

    AgentProfile disableAgent(String tenantId, String agentId, String actorId);

    void archiveAgent(String tenantId, String agentId, String actorId);

    void restoreAgent(String tenantId, String agentId, String actorId);

    void deleteAgent(String tenantId, String agentId);
}

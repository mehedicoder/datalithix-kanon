package ai.datalithix.kanon.config.model;

import ai.datalithix.kanon.common.AiTaskType;
import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.SourceType;
import java.util.Set;

public record AgentDefinition(
        String id,
        String displayName,
        String agentType,
        Set<DomainType> supportedDomains,
        Set<AiTaskType> supportedTaskTypes,
        Set<AssetType> supportedAssetTypes,
        Set<SourceType> supportedSourceTypes,
        String inputSchemaRef,
        String outputSchemaRef,
        String modelRoutePolicy
) {}

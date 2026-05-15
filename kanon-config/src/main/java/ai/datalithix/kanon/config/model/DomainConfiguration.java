package ai.datalithix.kanon.config.model;

import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.SourceType;
import java.util.List;
import java.util.Set;

public record DomainConfiguration(
        String id,
        String displayName,
        DomainType domainType,
        List<EntityDefinition> entities,
        List<FieldDefinition> fields,
        Set<String> ruleIds,
        Set<String> agentDefinitionIds,
        Set<String> workflowTemplateIds,
        Set<AssetType> supportedAssetTypes,
        Set<SourceType> supportedSourceTypes
) {}

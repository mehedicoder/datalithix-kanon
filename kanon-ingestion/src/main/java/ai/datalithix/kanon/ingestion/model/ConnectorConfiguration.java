package ai.datalithix.kanon.ingestion.model;

import ai.datalithix.kanon.common.SourceCategory;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import java.util.Map;

public record ConnectorConfiguration(
        String connectorId,
        String tenantId,
        String displayName,
        SourceCategory sourceCategory,
        SourceType sourceType,
        boolean enabled,
        ConnectorExecutionPolicy executionPolicy,
        Map<String, String> properties,
        Map<String, String> secretRefs,
        AuditMetadata audit
) {}

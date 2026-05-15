package ai.datalithix.kanon.config.model;

import ai.datalithix.kanon.common.SourceCategory;
import ai.datalithix.kanon.common.SourceType;
import java.util.Map;

public record ConnectorDefinition(
        String id,
        String displayName,
        SourceCategory sourceCategory,
        SourceType sourceType,
        boolean tenantScoped,
        boolean supportsAttachments,
        Map<String, String> defaults
) {}

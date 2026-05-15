package ai.datalithix.kanon.config.model;

import java.util.Set;

public record PolicyTemplate(
        String id,
        String displayName,
        Set<String> ruleIds,
        Set<String> complianceTags
) {}

package ai.datalithix.kanon.policy.security;

import ai.datalithix.kanon.common.compliance.DataClassification;
import ai.datalithix.kanon.common.security.Permission;
import java.util.Set;

public record RedactionPolicy(
        Set<DataClassification> sensitiveClassifications,
        Set<Permission> revealPermissions,
        String replacement
) {}

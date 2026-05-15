package ai.datalithix.kanon.config.model;

import ai.datalithix.kanon.common.model.AuditMetadata;
import java.time.Instant;

public record ActiveConfigurationVersion(
        String configurationId,
        String tenantId,
        ConfigurationType configurationType,
        String templateId,
        int version,
        ConfigurationActivationState activationState,
        String activatedBy,
        Instant activatedAt,
        String deactivatedBy,
        Instant deactivatedAt,
        String changeReason,
        AuditMetadata audit
) {}

package ai.datalithix.kanon.modelregistry.model;

import ai.datalithix.kanon.common.model.AuditMetadata;
import java.time.Instant;
import java.util.List;

public record DeploymentTarget(
        String deploymentTargetId,
        String modelVersionId,
        String modelEntryId,
        String tenantId,
        String targetType,
        String endpointUrl,
        String healthStatus,
        boolean healthy,
        Instant lastHealthCheckAt,
        String credentialRef,
        DeploymentConfig config,
        boolean active,
        Instant deployedAt,
        Instant rolledBackAt,
        List<String> evidenceEventIds,
        AuditMetadata audit
) {
    public DeploymentTarget {
        if (deploymentTargetId == null || deploymentTargetId.isBlank()) {
            throw new IllegalArgumentException("deploymentTargetId is required");
        }
        if (modelVersionId == null || modelVersionId.isBlank()) {
            throw new IllegalArgumentException("modelVersionId is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (audit == null) {
            throw new IllegalArgumentException("audit is required");
        }
        evidenceEventIds = evidenceEventIds == null ? List.of() : List.copyOf(evidenceEventIds);
    }
}

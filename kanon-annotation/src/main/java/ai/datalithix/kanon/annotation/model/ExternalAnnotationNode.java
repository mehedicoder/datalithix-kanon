package ai.datalithix.kanon.annotation.model;

import ai.datalithix.kanon.common.model.AuditMetadata;
import java.time.Instant;

public record ExternalAnnotationNode(
        String nodeId,
        String tenantId,
        String displayName,
        ExternalAnnotationProviderType providerType,
        String baseUrl,
        String secretRef,
        String storageBucket,
        ExternalAnnotationNodeStatus status,
        String lastKnownVersion,
        Long lastVerificationLatencyMs,
        Instant lastVerifiedAt,
        boolean enabled,
        AuditMetadata audit
) {
    public ExternalAnnotationNode {
        if (nodeId == null || nodeId.isBlank()) {
            throw new IllegalArgumentException("nodeId is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName is required");
        }
        if (providerType == null) {
            throw new IllegalArgumentException("providerType is required");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl is required");
        }
        if (secretRef == null || secretRef.isBlank()) {
            throw new IllegalArgumentException("secretRef is required");
        }
        if (audit == null) {
            throw new IllegalArgumentException("audit is required");
        }
    }
}

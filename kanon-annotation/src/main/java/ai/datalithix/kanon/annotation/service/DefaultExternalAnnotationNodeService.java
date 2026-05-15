package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.AnnotationNodeVerificationResult;
import ai.datalithix.kanon.annotation.model.ExternalAnnotationNode;
import ai.datalithix.kanon.annotation.model.ExternalAnnotationNodeStatus;
import ai.datalithix.kanon.annotation.model.ExternalAnnotationProviderType;
import ai.datalithix.kanon.common.ActorType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.evidence.model.EvidenceEvent;
import ai.datalithix.kanon.evidence.service.EvidenceLedger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class DefaultExternalAnnotationNodeService implements ExternalAnnotationNodeService {
    private final ExternalAnnotationNodeRepository repository;
    private final AnnotationNodeVerificationService verificationService;
    private final ExternalAnnotationNodeUsageGuard usageGuard;
    private final EvidenceLedger evidenceLedger;

    public DefaultExternalAnnotationNodeService(
            ExternalAnnotationNodeRepository repository,
            AnnotationNodeVerificationService verificationService,
            ExternalAnnotationNodeUsageGuard usageGuard,
            EvidenceLedger evidenceLedger
    ) {
        this.repository = repository;
        this.verificationService = verificationService;
        this.usageGuard = usageGuard;
        this.evidenceLedger = evidenceLedger;
    }

    @Override
    public List<ExternalAnnotationNode> list(String requesterTenantId, boolean platformScoped) {
        if (requesterTenantId == null || requesterTenantId.isBlank()) {
            return List.of();
        }
        if (platformScoped) {
            return repository.findPage(new ai.datalithix.kanon.common.model.QuerySpec(
                    requesterTenantId,
                    new ai.datalithix.kanon.common.model.PageSpec(0, 200, "updatedAt", ai.datalithix.kanon.common.model.SortDirection.DESC),
                    List.of(),
                    Map.of("platformScoped", "true")
            )).items();
        }
        return repository.findByTenant(requesterTenantId);
    }

    @Override
    public Optional<ExternalAnnotationNode> findById(String requesterTenantId, String nodeId, boolean platformScoped) {
        if (requesterTenantId == null || requesterTenantId.isBlank()) {
            return Optional.empty();
        }
        if (platformScoped) {
            return repository.findPage(new ai.datalithix.kanon.common.model.QuerySpec(
                    requesterTenantId,
                    new ai.datalithix.kanon.common.model.PageSpec(0, 200, "updatedAt", ai.datalithix.kanon.common.model.SortDirection.DESC),
                    List.of(),
                    Map.of("platformScoped", "true")
            )).items().stream().filter(node -> node.nodeId().equals(nodeId)).findFirst();
        }
        return repository.findById(requesterTenantId, nodeId);
    }

    @Override
    public ExternalAnnotationNode create(
            String requesterTenantId,
            boolean platformScoped,
            String tenantId,
            String displayName,
            ExternalAnnotationProviderType providerType,
            String baseUrl,
            String secretRef,
            String storageBucket,
            String actorId
    ) {
        enforceTenantScope(requesterTenantId, platformScoped, tenantId);
        validateSecretRef(secretRef);
        Instant now = Instant.now();
        ExternalAnnotationNode node = new ExternalAnnotationNode(
                UUID.randomUUID().toString(),
                tenantId,
                displayName,
                providerType,
                baseUrl,
                secretRef,
                emptyToNull(storageBucket),
                ExternalAnnotationNodeStatus.OFFLINE,
                null,
                null,
                null,
                true,
                new AuditMetadata(now, actorId, now, actorId, 0)
        );
        ExternalAnnotationNode saved = repository.save(node);
        appendEvidence("ANNOTATION_NODE_CREATED", tenantId, null, Map.of(), Map.of("nodeId", saved.nodeId(), "providerType", saved.providerType().name()), actorId);
        return saved;
    }

    @Override
    public ExternalAnnotationNode update(
            String requesterTenantId,
            boolean platformScoped,
            String nodeId,
            String displayName,
            String baseUrl,
            String secretRef,
            String storageBucket,
            boolean enabled,
            String actorId
    ) {
        ExternalAnnotationNode existing = findExisting(requesterTenantId, platformScoped, nodeId);
        validateSecretRef(secretRef);
        ExternalAnnotationNode updated = new ExternalAnnotationNode(
                existing.nodeId(),
                existing.tenantId(),
                displayName,
                existing.providerType(),
                baseUrl,
                secretRef,
                emptyToNull(storageBucket),
                existing.status(),
                existing.lastKnownVersion(),
                existing.lastVerificationLatencyMs(),
                existing.lastVerifiedAt(),
                enabled,
                new AuditMetadata(existing.audit().createdAt(), existing.audit().createdBy(), Instant.now(), actorId, existing.audit().version() + 1)
        );
        ExternalAnnotationNode saved = repository.save(updated);
        appendEvidence("ANNOTATION_NODE_UPDATED", saved.tenantId(), null, Map.of("nodeId", existing.nodeId()), Map.of("nodeId", saved.nodeId()), actorId);
        return saved;
    }

    @Override
    public AnnotationNodeVerificationResult testConnection(String requesterTenantId, boolean platformScoped, String nodeId, String actorId) {
        ExternalAnnotationNode existing = findExisting(requesterTenantId, platformScoped, nodeId);
        AnnotationNodeVerificationResult result = verificationService.verify(existing);
        ExternalAnnotationNode updated = new ExternalAnnotationNode(
                existing.nodeId(),
                existing.tenantId(),
                existing.displayName(),
                existing.providerType(),
                existing.baseUrl(),
                existing.secretRef(),
                existing.storageBucket(),
                result.resultingStatus(),
                result.detectedVersion(),
                result.totalLatencyMs(),
                result.verifiedAt(),
                existing.enabled(),
                new AuditMetadata(existing.audit().createdAt(), existing.audit().createdBy(), Instant.now(), actorId, existing.audit().version() + 1)
        );
        repository.save(updated);
        appendEvidence(
                "ANNOTATION_NODE_TESTED",
                existing.tenantId(),
                null,
                Map.of("nodeId", existing.nodeId()),
                Map.of(
                        "nodeId", existing.nodeId(),
                        "result", result.resultingStatus().name(),
                        "latencyMs", result.totalLatencyMs(),
                        "version", result.detectedVersion() == null ? "unknown" : result.detectedVersion()
                ),
                actorId
        );
        return result;
    }

    @Override
    public void delete(String requesterTenantId, boolean platformScoped, String nodeId, String actorId) {
        ExternalAnnotationNode existing = findExisting(requesterTenantId, platformScoped, nodeId);
        long activeLinks = usageGuard.countActiveNonSyncedTasks(existing.tenantId(), nodeId);
        if (activeLinks > 0) {
            throw new IllegalStateException("Cannot delete annotation node linked to active non-synced workflow tasks");
        }
        repository.deleteById(existing.tenantId(), nodeId);
        appendEvidence("ANNOTATION_NODE_UPDATED", existing.tenantId(), null, Map.of("nodeId", existing.nodeId()), Map.of("deleted", true), actorId);
    }

    private ExternalAnnotationNode findExisting(String requesterTenantId, boolean platformScoped, String nodeId) {
        return findById(requesterTenantId, nodeId, platformScoped)
                .orElseThrow(() -> new IllegalArgumentException("External annotation node not found"));
    }

    private void enforceTenantScope(String requesterTenantId, boolean platformScoped, String targetTenantId) {
        if (requesterTenantId == null || requesterTenantId.isBlank()) {
            throw new SecurityException("Missing tenant scope");
        }
        if (!platformScoped && !requesterTenantId.equals(targetTenantId)) {
            throw new SecurityException("Tenant-scoped administrators cannot access nodes from other tenants");
        }
    }

    private static void validateSecretRef(String secretRef) {
        if (secretRef == null || secretRef.isBlank()) {
            throw new IllegalArgumentException("secretRef is required");
        }
        if (!secretRef.startsWith("env:") && !secretRef.startsWith("secret:")) {
            throw new IllegalArgumentException("secretRef must use env: or secret: references");
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private void appendEvidence(
            String eventType,
            String tenantId,
            String caseId,
            Map<String, Object> beforeState,
            Map<String, Object> afterState,
            String actorId
    ) {
        evidenceLedger.append(new EvidenceEvent(
                UUID.randomUUID().toString(),
                tenantId,
                caseId,
                eventType,
                ActorType.HUMAN,
                actorId,
                "external-annotation-node-service",
                null,
                null,
                beforeState,
                afterState,
                "External annotation node administration",
                Instant.now()
        ));
    }
}

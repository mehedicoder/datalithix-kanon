package ai.datalithix.kanon.modelregistry.service;

import ai.datalithix.kanon.common.ActorType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.evidence.model.EvidenceEvent;
import ai.datalithix.kanon.evidence.service.EvidenceLedger;
import ai.datalithix.kanon.modelregistry.model.DeploymentConfig;
import ai.datalithix.kanon.modelregistry.model.DeploymentTarget;
import ai.datalithix.kanon.modelregistry.model.ModelLifecycleStage;
import ai.datalithix.kanon.modelregistry.model.ModelVersion;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class DefaultDeploymentService implements DeploymentService {
    private final ModelRegistryRepository repository;
    private final EvidenceLedger evidenceLedger;
    private final Map<String, DeploymentTargetAdapter> deploymentAdapters;

    public DefaultDeploymentService(
            ModelRegistryRepository repository,
            EvidenceLedger evidenceLedger,
            List<DeploymentTargetAdapter> adapters
    ) {
        this.repository = repository;
        this.evidenceLedger = evidenceLedger;
        this.deploymentAdapters = adapters.stream()
                .collect(Collectors.toUnmodifiableMap(DeploymentTargetAdapter::supportedTargetType, a -> a));
    }

    @Override
    public DeploymentTarget deploy(String tenantId, String modelVersionId, String targetType,
                                    String endpointUrl, DeploymentConfig config, String actorId) {
        ModelVersion version = repository.findVersionById(tenantId, modelVersionId)
                .orElseThrow(() -> new IllegalArgumentException("Model version not found: " + modelVersionId));
        if (version.lifecycleStage() != ModelLifecycleStage.PRODUCTION
                && version.lifecycleStage() != ModelLifecycleStage.STAGING) {
            throw new IllegalStateException("Only STAGING or PRODUCTION models can be deployed");
        }
        String deploymentId = "dt-" + UUID.randomUUID();
        Instant now = Instant.now();
        AuditMetadata audit = new AuditMetadata(now, actorId, now, actorId, 1);
        var adapter = deploymentAdapters.get(targetType);
        DeploymentTargetAdapter.DeployResult deployResult = null;
        String healthStatus = "UNKNOWN";
        boolean healthy = true;
        if (adapter != null) {
            deployResult = adapter.deploy(version, endpointUrl, config);
            healthStatus = deployResult.healthStatus();
            healthy = deployResult.success();
        }
        DeploymentTarget target = new DeploymentTarget(deploymentId, modelVersionId, version.modelEntryId(),
                tenantId, targetType, endpointUrl, healthStatus, healthy, now, null,
                config, healthy, now, null, List.of(), audit);
        DeploymentTarget saved = repository.saveDeployment(target);
        List<String> depIds = new ArrayList<>(version.deploymentTargetIds());
        depIds.add(deploymentId);
        ModelVersion updated = new ModelVersion(
                version.modelVersionId(), version.modelEntryId(), version.tenantId(), version.versionNumber(),
                version.trainingJobId(), version.datasetVersionId(), version.datasetDefinitionId(),
                version.artifact(), version.hyperParameters(), version.complianceTags(),
                version.lifecycleStage(), version.promotedBy(), version.promotedAt(),
                version.evaluationRunIds(), List.copyOf(depIds), version.evidenceEventIds(),
                updatedAudit(version.audit(), actorId));
        repository.saveVersion(updated);
        evidenceLedger.append(new EvidenceEvent(UUID.randomUUID().toString(), tenantId, modelVersionId,
                "MODEL_DEPLOYED", ActorType.SYSTEM, actorId, "deployment-service",
                null, null, Map.of(), Map.of("deploymentId", deploymentId, "targetType", targetType),
                "Model deployed to " + targetType, now));
        return saved;
    }

    @Override
    public DeploymentTarget rollback(String tenantId, String deploymentTargetId, String actorId) {
        DeploymentTarget target = repository.findDeploymentById(tenantId, deploymentTargetId)
                .orElseThrow(() -> new IllegalArgumentException("Deployment not found: " + deploymentTargetId));
        if (!target.active()) {
            throw new IllegalStateException("Deployment is already inactive");
        }
        var adapter = deploymentAdapters.get(target.targetType());
        if (adapter != null && target.endpointUrl() != null) {
            adapter.rollback(target.endpointUrl(), target.deploymentTargetId());
        }
        Instant now = Instant.now();
        DeploymentTarget rolledBack = new DeploymentTarget(
                target.deploymentTargetId(), target.modelVersionId(), target.modelEntryId(),
                target.tenantId(), target.targetType(), target.endpointUrl(), "ROLLED_BACK",
                false, now, target.credentialRef(), target.config(), false,
                target.deployedAt(), now, target.evidenceEventIds(), updatedAudit(target.audit(), actorId));
        repository.saveDeployment(rolledBack);
        evidenceLedger.append(new EvidenceEvent(UUID.randomUUID().toString(), tenantId, target.modelVersionId(),
                "MODEL_ROLLED_BACK", ActorType.SYSTEM, actorId, "deployment-service",
                null, null, Map.of("active", "true"), Map.of("active", "false"),
                "Deployment rolled back", now));
        return rolledBack;
    }

    @Override
    public boolean healthCheck(String tenantId, String deploymentTargetId) {
        DeploymentTarget target = repository.findDeploymentById(tenantId, deploymentTargetId)
                .orElseThrow(() -> new IllegalArgumentException("Deployment not found: " + deploymentTargetId));
        var adapter = deploymentAdapters.get(target.targetType());
        boolean healthy;
        if (adapter != null && target.endpointUrl() != null) {
            healthy = adapter.healthCheck(target.endpointUrl());
        } else {
            healthy = target.endpointUrl() != null && !target.endpointUrl().isBlank();
        }
        Instant now = Instant.now();
        DeploymentTarget checked = new DeploymentTarget(
                target.deploymentTargetId(), target.modelVersionId(), target.modelEntryId(),
                target.tenantId(), target.targetType(), target.endpointUrl(),
                healthy ? "HEALTHY" : "UNHEALTHY", healthy, now,
                target.credentialRef(), target.config(), target.active(),
                target.deployedAt(), target.rolledBackAt(), target.evidenceEventIds(),
                updatedAudit(target.audit(), "health-check"));
        repository.saveDeployment(checked);
        return healthy;
    }

    @Override
    public List<DeploymentTarget> getActiveDeployments(String tenantId) {
        return repository.findActiveDeployments(tenantId);
    }

    @Override
    public List<DeploymentTarget> getDeploymentsByVersion(String tenantId, String modelVersionId) {
        return repository.findDeploymentsByVersion(tenantId, modelVersionId);
    }

    private static AuditMetadata updatedAudit(AuditMetadata existing, String actorId) {
        return new AuditMetadata(existing.createdAt(), existing.createdBy(),
                Instant.now(), actorId, existing.version() + 1);
    }

}

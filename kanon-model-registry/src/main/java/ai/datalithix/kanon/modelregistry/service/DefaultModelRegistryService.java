package ai.datalithix.kanon.modelregistry.service;

import ai.datalithix.kanon.common.ActorType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.evidence.model.EvidenceEvent;
import ai.datalithix.kanon.evidence.service.EvidenceLedger;
import ai.datalithix.kanon.modelregistry.model.ModelArtifact;
import ai.datalithix.kanon.modelregistry.model.ModelEntry;
import ai.datalithix.kanon.modelregistry.model.ModelLifecycleStage;
import ai.datalithix.kanon.modelregistry.model.ModelVersion;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DefaultModelRegistryService implements ModelRegistryService {
    private final ModelRegistryRepository repository;
    private final EvidenceLedger evidenceLedger;

    public DefaultModelRegistryService(ModelRegistryRepository repository, EvidenceLedger evidenceLedger) {
        this.repository = repository;
        this.evidenceLedger = evidenceLedger;
    }

    @Override
    public ModelEntry registerModel(String tenantId, String modelName, String framework, String taskType,
                                     String domainType, String artifactUri, String trainingJobId,
                                     String datasetVersionId, String actorId) {
        String entryId = "me-" + UUID.randomUUID();
        String versionId = "mv-" + UUID.randomUUID();
        Instant now = Instant.now();
        AuditMetadata entryAudit = new AuditMetadata(now, actorId, now, actorId, 1);
        AuditMetadata versionAudit = new AuditMetadata(now, actorId, now, actorId, 1);
        ModelEntry entry = new ModelEntry(entryId, tenantId, modelName, "Registered from training job " + trainingJobId,
                framework, taskType, domainType, java.util.Set.of(), 1, ModelLifecycleStage.DEVELOPMENT.name(),
                versionId, true, List.of(versionId), entryAudit);
        ModelArtifact artifact = new ModelArtifact(artifactUri, framework, 0, null, "S3");
        ModelVersion version = new ModelVersion(versionId, entryId, tenantId, 1, trainingJobId,
                datasetVersionId, null, artifact, null, java.util.Set.of(),
                ModelLifecycleStage.DEVELOPMENT, actorId, now, List.of(), List.of(), List.of(), versionAudit);
        repository.saveEntry(entry);
        repository.saveVersion(version);
        evidenceLedger.append(new EvidenceEvent(UUID.randomUUID().toString(), tenantId, entryId,
                "MODEL_REGISTERED", ActorType.SYSTEM, actorId, "model-registry-service",
                null, null, Map.of(), Map.of("modelName", modelName, "versionId", versionId),
                "Model " + modelName + " registered", now));
        return entry;
    }

    @Override
    public ModelVersion promoteModel(String tenantId, String modelVersionId, ModelLifecycleStage targetStage,
                                      String actorId) {
        ModelVersion version = repository.findVersionById(tenantId, modelVersionId)
                .orElseThrow(() -> new IllegalArgumentException("Model version not found: " + modelVersionId));
        if (version.lifecycleStage() == targetStage) {
            throw new IllegalStateException("Model version " + modelVersionId + " is already in stage " + targetStage);
        }
        if (version.lifecycleStage() == ModelLifecycleStage.ARCHIVED) {
            throw new IllegalStateException("Cannot promote an archived model version");
        }
        Instant now = Instant.now();
        ModelVersion promoted = new ModelVersion(
                version.modelVersionId(), version.modelEntryId(), version.tenantId(), version.versionNumber(),
                version.trainingJobId(), version.datasetVersionId(), version.datasetDefinitionId(),
                version.artifact(), version.hyperParameters(), version.complianceTags(),
                targetStage, actorId, now, version.evaluationRunIds(), version.deploymentTargetIds(),
                version.evidenceEventIds(), updatedAudit(version.audit(), actorId)
        );
        repository.saveVersion(promoted);
        ModelEntry entry = repository.findEntryById(tenantId, version.modelEntryId()).orElseThrow();
        repository.saveEntry(new ModelEntry(
                entry.modelEntryId(), entry.tenantId(), entry.modelName(), entry.description(),
                entry.framework(), entry.taskType(), entry.domainType(), entry.complianceTags(),
                entry.latestVersionNumber(), targetStage.name(), entry.latestVersionId(),
                entry.enabled(), entry.versionIds(), updatedAudit(entry.audit(), actorId)
        ));
        evidenceLedger.append(new EvidenceEvent(UUID.randomUUID().toString(), tenantId, version.modelEntryId(),
                "MODEL_PROMOTED", ActorType.SYSTEM, actorId, "model-registry-service",
                null, null, Map.of("fromStage", version.lifecycleStage().name()), Map.of("toStage", targetStage.name()),
                "Model version " + version.versionNumber() + " promoted to " + targetStage, now));
        return promoted;
    }

    @Override
    public ModelVersion rollbackModel(String tenantId, String modelEntryId, int targetVersionNumber, String actorId) {
        List<ModelVersion> versions = repository.findVersionsByEntryId(tenantId, modelEntryId);
        ModelVersion target = versions.stream()
                .filter(v -> v.versionNumber() == targetVersionNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Version " + targetVersionNumber + " not found"));
        if (target.lifecycleStage() == ModelLifecycleStage.ARCHIVED) {
            throw new IllegalStateException("Cannot rollback to an archived version");
        }
        return promoteModel(tenantId, target.modelVersionId(), ModelLifecycleStage.PRODUCTION, actorId);
    }

    @Override
    public ModelEntry getModelEntry(String tenantId, String modelEntryId) {
        return repository.findEntryById(tenantId, modelEntryId)
                .orElseThrow(() -> new IllegalArgumentException("Model entry not found: " + modelEntryId));
    }

    @Override
    public ModelVersion getModelVersion(String tenantId, String modelVersionId) {
        return repository.findVersionById(tenantId, modelVersionId)
                .orElseThrow(() -> new IllegalArgumentException("Model version not found: " + modelVersionId));
    }

    @Override
    public List<ModelEntry> listModels(String tenantId) {
        return repository.findEntriesByTenant(tenantId);
    }

    @Override
    public List<ModelVersion> listVersions(String tenantId, String modelEntryId) {
        return repository.findVersionsByEntryId(tenantId, modelEntryId);
    }

    @Override
    public List<ModelVersion> listModelsByStage(String tenantId, ModelLifecycleStage stage) {
        return repository.findVersionsByStage(tenantId, stage);
    }

    private static AuditMetadata updatedAudit(AuditMetadata existing, String actorId) {
        return new AuditMetadata(existing.createdAt(), existing.createdBy(),
                Instant.now(), actorId, existing.version() + 1);
    }

}

package ai.datalithix.kanon.training.service;

import ai.datalithix.kanon.common.ActorType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.dataset.model.DatasetDefinition;
import ai.datalithix.kanon.dataset.model.DatasetVersion;
import ai.datalithix.kanon.dataset.service.DatasetRepository;
import ai.datalithix.kanon.evidence.model.EvidenceEvent;
import ai.datalithix.kanon.evidence.service.EvidenceLedger;
import ai.datalithix.kanon.training.model.ComputeBackend;
import ai.datalithix.kanon.training.model.ComputeBackendType;
import ai.datalithix.kanon.training.model.HyperParameterConfig;
import ai.datalithix.kanon.training.model.TrainingJob;
import ai.datalithix.kanon.training.model.TrainingJobStatus;
import ai.datalithix.kanon.training.model.TrainingMetrics;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DefaultTrainingOrchestrationService implements TrainingOrchestrationService {
    private final TrainingJobRepository trainingJobRepository;
    private final DatasetRepository datasetRepository;
    private final EvidenceLedger evidenceLedger;
    private final Map<ComputeBackendType, ComputeBackendAdapter> backendAdapters;

    public DefaultTrainingOrchestrationService(
            TrainingJobRepository trainingJobRepository,
            DatasetRepository datasetRepository,
            EvidenceLedger evidenceLedger,
            List<ComputeBackendAdapter> adapters
    ) {
        this.trainingJobRepository = trainingJobRepository;
        this.datasetRepository = datasetRepository;
        this.evidenceLedger = evidenceLedger;
        this.backendAdapters = adapters.stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        a -> a.supportedBackendType(), a -> a
                ));
    }

    @Override
    public TrainingJob submitJob(String tenantId, String datasetVersionId, String datasetDefinitionId,
                                  String computeBackendId, String modelName,
                                  HyperParameterConfig hyperParameters, String actorId) {
        DatasetVersion version = datasetRepository.findVersionById(tenantId, datasetVersionId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset version not found: " + datasetVersionId));
        ComputeBackend backend = trainingJobRepository.findBackendById(tenantId, computeBackendId)
                .orElseThrow(() -> new IllegalArgumentException("Compute backend not found: " + computeBackendId));
        ComputeBackendAdapter adapter = backendAdapters.get(backend.backendType());
        if (adapter == null) {
            throw new IllegalArgumentException("No adapter for backend type: " + backend.backendType());
        }
        String jobId = "tj-" + UUID.randomUUID();
        Instant now = Instant.now();
        AuditMetadata audit = new AuditMetadata(now, actorId, now, actorId, 1);
        String datasetExportUri = "s3://datasets/" + tenantId + "/" + datasetDefinitionId
                + "/v" + version.versionNumber() + "/export.jsonl";
        String externalJobId = adapter.submitJob(
                new TrainingJob(jobId, tenantId, datasetVersionId, datasetDefinitionId, computeBackendId,
                        modelName, hyperParameters, TrainingJobStatus.QUEUED, now, null, null, null,
                        null, null, null, List.of(), 0, null, List.of(), audit),
                backend, datasetExportUri);
        TrainingJob job = new TrainingJob(jobId, tenantId, datasetVersionId, datasetDefinitionId,
                computeBackendId, modelName, hyperParameters, TrainingJobStatus.QUEUED,
                now, null, null, null, null, null, null, List.of(), 0,
                externalJobId, List.of(), audit);
        TrainingJob saved = trainingJobRepository.save(job);
        evidenceLedger.append(new EvidenceEvent(UUID.randomUUID().toString(), tenantId, jobId,
                "TRAINING_JOB_SUBMITTED", ActorType.SYSTEM, actorId, "training-orchestration-service",
                null, null, Map.of(), Map.of("modelName", modelName, "backend", computeBackendId),
                "Training job submitted for model " + modelName, now));
        return saved;
    }

    @Override
    public TrainingJob cancelJob(String tenantId, String trainingJobId) {
        TrainingJob job = trainingJobRepository.findById(tenantId, trainingJobId)
                .orElseThrow(() -> new IllegalArgumentException("Training job not found: " + trainingJobId));
        if (job.status() == TrainingJobStatus.COMPLETED || job.status() == TrainingJobStatus.FAILED
                || job.status() == TrainingJobStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel job in state: " + job.status());
        }
        ComputeBackend backend = trainingJobRepository.findBackendById(tenantId, job.computeBackendId())
                .orElse(null);
        if (backend != null && job.externalJobId() != null) {
            ComputeBackendAdapter adapter = backendAdapters.get(backend.backendType());
            if (adapter != null) {
                adapter.cancelJob(job.externalJobId(), backend);
            }
        }
        TrainingJob cancelled = new TrainingJob(
                job.trainingJobId(), job.tenantId(), job.datasetVersionId(), job.datasetDefinitionId(),
                job.computeBackendId(), job.modelName(), job.hyperParameters(), TrainingJobStatus.CANCELLED,
                job.requestedAt(), job.startedAt(), null, Instant.now(), "Cancelled by " + trainingJobId,
                job.checkpointUri(), job.outputModelArtifactUri(), job.metricsHistory(),
                job.totalDurationSeconds(), job.externalJobId(), job.evidenceEventIds(),
                updatedAudit(job.audit(), trainingJobId));
        trainingJobRepository.save(cancelled);
        evidenceLedger.append(new EvidenceEvent(UUID.randomUUID().toString(), tenantId, trainingJobId,
                "TRAINING_JOB_CANCELLED", ActorType.SYSTEM, trainingJobId, "training-orchestration-service",
                null, null, Map.of("status", job.status().name()), Map.of("status", TrainingJobStatus.CANCELLED.name()),
                "Training job cancelled", Instant.now()));
        return cancelled;
    }

    @Override
    public TrainingJob updateJobStatus(String tenantId, String trainingJobId,
                                       TrainingJobStatus newStatus, String actorId) {
        TrainingJob job = trainingJobRepository.findById(tenantId, trainingJobId)
                .orElseThrow(() -> new IllegalArgumentException("Training job not found: " + trainingJobId));
        if (job.status() == TrainingJobStatus.COMPLETED || job.status() == TrainingJobStatus.FAILED
                || job.status() == TrainingJobStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update terminal job in state: " + job.status());
        }
        Instant now = Instant.now();
        TrainingJob updated = new TrainingJob(
                job.trainingJobId(), job.tenantId(), job.datasetVersionId(), job.datasetDefinitionId(),
                job.computeBackendId(), job.modelName(), job.hyperParameters(), newStatus,
                job.requestedAt(), job.startedAt() != null ? job.startedAt() : now,
                newStatus == TrainingJobStatus.COMPLETED ? now : null,
                newStatus == TrainingJobStatus.FAILED ? now : job.failedAt(),
                newStatus == TrainingJobStatus.FAILED ? "Job failed" : job.failureReason(),
                job.checkpointUri(), job.outputModelArtifactUri(), job.metricsHistory(),
                job.totalDurationSeconds(), job.externalJobId(), job.evidenceEventIds(),
                updatedAudit(job.audit(), actorId));
        trainingJobRepository.save(updated);
        String eventType = newStatus == TrainingJobStatus.COMPLETED
                ? "TRAINING_JOB_COMPLETED"
                : newStatus == TrainingJobStatus.FAILED
                ? "TRAINING_JOB_FAILED"
                : "TRAINING_JOB_STATUS_CHANGED";
        evidenceLedger.append(new EvidenceEvent(UUID.randomUUID().toString(), tenantId, trainingJobId,
                eventType, ActorType.SYSTEM, actorId, "training-orchestration-service",
                null, null,
                Map.of("previousStatus", job.status().name()),
                Map.of("newStatus", newStatus.name()),
                "Training job " + newStatus.name().toLowerCase() + " for job " + trainingJobId, now));
        return updated;
    }

    @Override
    public TrainingJob getJobStatus(String tenantId, String trainingJobId) {
        return trainingJobRepository.findById(tenantId, trainingJobId)
                .orElseThrow(() -> new IllegalArgumentException("Training job not found: " + trainingJobId));
    }

    @Override
    public List<TrainingJob> listJobs(String tenantId) {
        return trainingJobRepository.findByTenant(tenantId);
    }

    @Override
    public List<TrainingJob> listJobsByDataset(String tenantId, String datasetVersionId) {
        return trainingJobRepository.findByDatasetVersion(tenantId, datasetVersionId);
    }

    @Override
    public ComputeBackend registerBackend(ComputeBackend backend) {
        ComputeBackendAdapter adapter = backendAdapters.get(backend.backendType());
        if (adapter == null) {
            throw new IllegalArgumentException("No adapter for backend type: " + backend.backendType());
        }
        boolean healthy = adapter.healthCheck(backend);
        ComputeBackend updated = new ComputeBackend(
                backend.backendId(), backend.tenantId(), backend.backendType(), backend.name(),
                backend.endpointUrl(), backend.credentialRef(), backend.configuration(),
                backend.enabled(), healthy, Instant.now().toString(),
                healthy ? null : "Health check failed"
        );
        trainingJobRepository.saveBackend(updated);
        return updated;
    }

    @Override
    public boolean healthCheckBackend(String tenantId, String backendId) {
        ComputeBackend backend = trainingJobRepository.findBackendById(tenantId, backendId)
                .orElseThrow(() -> new IllegalArgumentException("Backend not found: " + backendId));
        ComputeBackendAdapter adapter = backendAdapters.get(backend.backendType());
        if (adapter == null) return false;
        return adapter.healthCheck(backend);
    }

    private static AuditMetadata updatedAudit(AuditMetadata existing, String actorId) {
        return new AuditMetadata(existing.createdAt(), existing.createdBy(),
                Instant.now(), actorId, existing.version() + 1);
    }

}

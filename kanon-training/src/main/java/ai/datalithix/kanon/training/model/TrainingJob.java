package ai.datalithix.kanon.training.model;

import ai.datalithix.kanon.common.model.AuditMetadata;
import java.time.Instant;
import java.util.List;

public record TrainingJob(
        String trainingJobId,
        String tenantId,
        String datasetVersionId,
        String datasetDefinitionId,
        String computeBackendId,
        String modelName,
        HyperParameterConfig hyperParameters,
        TrainingJobStatus status,
        Instant requestedAt,
        Instant startedAt,
        Instant completedAt,
        Instant failedAt,
        String failureReason,
        String checkpointUri,
        String outputModelArtifactUri,
        List<TrainingMetrics> metricsHistory,
        long totalDurationSeconds,
        String externalJobId,
        List<String> evidenceEventIds,
        AuditMetadata audit
) {
    public TrainingJob {
        if (trainingJobId == null || trainingJobId.isBlank()) {
            throw new IllegalArgumentException("trainingJobId is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (hyperParameters == null) {
            throw new IllegalArgumentException("hyperParameters is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (audit == null) {
            throw new IllegalArgumentException("audit is required");
        }
        metricsHistory = metricsHistory == null ? List.of() : List.copyOf(metricsHistory);
        evidenceEventIds = evidenceEventIds == null ? List.of() : List.copyOf(evidenceEventIds);
    }
}

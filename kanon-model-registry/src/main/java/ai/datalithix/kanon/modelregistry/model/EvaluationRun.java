package ai.datalithix.kanon.modelregistry.model;

import ai.datalithix.kanon.common.model.AuditMetadata;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record EvaluationRun(
        String evaluationRunId,
        String modelVersionId,
        String modelEntryId,
        String tenantId,
        String testDatasetVersionId,
        List<EvaluationMetric> metrics,
        Map<String, Double> perClassMetrics,
        String confusionMatrixUri,
        String failureCaseSampleUri,
        String status,
        String failureReason,
        Instant startedAt,
        Instant completedAt,
        boolean passedThreshold,
        List<String> evidenceEventIds,
        AuditMetadata audit
) {
    public EvaluationRun {
        if (evaluationRunId == null || evaluationRunId.isBlank()) {
            throw new IllegalArgumentException("evaluationRunId is required");
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
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
        perClassMetrics = perClassMetrics == null ? Map.of() : Map.copyOf(perClassMetrics);
        evidenceEventIds = evidenceEventIds == null ? List.of() : List.copyOf(evidenceEventIds);
    }
}

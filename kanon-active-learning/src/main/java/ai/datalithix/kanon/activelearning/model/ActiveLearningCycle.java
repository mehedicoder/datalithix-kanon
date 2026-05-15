package ai.datalithix.kanon.activelearning.model;

import ai.datalithix.kanon.common.model.AuditMetadata;
import java.time.Instant;
import java.util.List;

public record ActiveLearningCycle(
        String cycleId,
        String tenantId,
        String modelEntryId,
        String modelVersionId,
        SelectionStrategyType strategyType,
        CycleStatus status,
        int selectedRecordCount,
        int passedReviewCount,
        String sourceDatasetVersionId,
        String targetDatasetVersionId,
        String retrainingJobId,
        String evaluationRunId,
        String rejectionReason,
        Instant startedAt,
        Instant completedAt,
        String cronExpression,
        boolean autoTrigger,
        List<String> evidenceEventIds,
        AuditMetadata audit
) {
    public ActiveLearningCycle {
        if (cycleId == null || cycleId.isBlank()) throw new IllegalArgumentException("cycleId is required");
        if (tenantId == null || tenantId.isBlank()) throw new IllegalArgumentException("tenantId is required");
        if (strategyType == null) throw new IllegalArgumentException("strategyType is required");
        if (status == null) throw new IllegalArgumentException("status is required");
        if (audit == null) throw new IllegalArgumentException("audit is required");
        evidenceEventIds = evidenceEventIds == null ? List.of() : List.copyOf(evidenceEventIds);
    }
}

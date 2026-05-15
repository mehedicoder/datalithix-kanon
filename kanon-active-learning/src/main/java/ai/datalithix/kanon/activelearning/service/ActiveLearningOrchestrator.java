package ai.datalithix.kanon.activelearning.service;

import ai.datalithix.kanon.activelearning.model.ActiveLearningConfig;
import ai.datalithix.kanon.activelearning.model.ActiveLearningCycle;
import ai.datalithix.kanon.activelearning.model.CycleStatus;
import ai.datalithix.kanon.activelearning.model.SelectedRecord;
import ai.datalithix.kanon.activelearning.model.SelectionStrategyType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.evidence.model.EvidenceEvent;
import ai.datalithix.kanon.evidence.service.EvidenceLedger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ActiveLearningOrchestrator {
    private final ActiveLearningCycleRepository cycleRepository;
    private final EvidenceLedger evidenceLedger;
    private final Map<SelectionStrategyType, RecordSelector> selectors;

    public ActiveLearningOrchestrator(
            ActiveLearningCycleRepository cycleRepository,
            EvidenceLedger evidenceLedger,
            List<RecordSelector> selectorList
    ) {
        this.cycleRepository = cycleRepository;
        this.evidenceLedger = evidenceLedger;
        this.selectors = selectorList.stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        RecordSelector::supportedStrategy, s -> s
                ));
    }

    public ActiveLearningCycle startCycle(
            String tenantId,
            String modelEntryId,
            String modelVersionId,
            String sourceDatasetVersionId,
            ActiveLearningConfig config,
            List<RecordSelector.RecordCandidate> candidates,
            String actorId
    ) {
        RecordSelector selector = selectors.get(config.strategyType());
        if (selector == null) {
            throw new IllegalArgumentException("No selector for strategy: " + config.strategyType());
        }
        String cycleId = "alc-" + UUID.randomUUID();
        Instant now = Instant.now();
        AuditMetadata audit = new AuditMetadata(now, actorId, now, actorId, 1);

        List<SelectedRecord> selected = selector.selectRecords(candidates, config);
        int selectedCount = selected.size();

        ActiveLearningCycle cycle = new ActiveLearningCycle(
                cycleId, tenantId, modelEntryId, modelVersionId,
                config.strategyType(), CycleStatus.AWAITING_REVIEW,
                selectedCount, 0, sourceDatasetVersionId, null,
                null, null, null, now, null,
                config.scheduleCron(), config.autoTriggerRetraining(),
                List.of(), audit
        );
        ActiveLearningCycle saved = cycleRepository.save(cycle);
        evidenceLedger.append(new EvidenceEvent(
                UUID.randomUUID().toString(), tenantId, modelEntryId,
                "ACTIVE_LEARNING_CYCLE_STARTED", ai.datalithix.kanon.common.ActorType.SYSTEM, actorId,
                "active-learning-orchestrator", null, null,
                Map.of(), Map.of("cycleId", cycleId, "strategy", config.strategyType().name(),
                        "recordsSelected", selectedCount),
                "Active learning cycle started: " + config.strategyType(), now
        ));
        return saved;
    }

    public ActiveLearningCycle updateStatus(String tenantId, String cycleId, CycleStatus newStatus,
                                             String targetDatasetVersionId, String actorId) {
        ActiveLearningCycle cycle = cycleRepository.findById(tenantId, cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found: " + cycleId));
        if (!isValidTransition(cycle.status(), newStatus)) {
            throw new IllegalStateException("Cannot transition from " + cycle.status() + " to " + newStatus);
        }
        Instant now = Instant.now();
        ActiveLearningCycle updated = new ActiveLearningCycle(
                cycle.cycleId(), cycle.tenantId(), cycle.modelEntryId(), cycle.modelVersionId(),
                cycle.strategyType(), newStatus, cycle.selectedRecordCount(), cycle.passedReviewCount(),
                cycle.sourceDatasetVersionId(),
                newStatus == CycleStatus.DATASET_UPDATED ? targetDatasetVersionId : cycle.targetDatasetVersionId(),
                cycle.selectedRecordCount() >= 0 ? null : cycle.retrainingJobId(),
                cycle.evaluationRunId(), null, cycle.startedAt(),
                (newStatus == CycleStatus.PROMOTED || newStatus == CycleStatus.REJECTED
                        || newStatus == CycleStatus.CANCELLED) ? now : null,
                cycle.cronExpression(), cycle.autoTrigger(),
                cycle.evidenceEventIds(), updatedAudit(cycle.audit(), actorId)
        );
        cycleRepository.save(updated);
        evidenceLedger.append(new EvidenceEvent(
                UUID.randomUUID().toString(), tenantId, cycleId,
                "ACTIVE_LEARNING_CYCLE_" + newStatus.name(),
                ai.datalithix.kanon.common.ActorType.SYSTEM, actorId,
                "active-learning-orchestrator", null, null,
                Map.of("fromStatus", cycle.status().name()), Map.of("toStatus", newStatus.name()),
                "Active learning cycle transitioned to " + newStatus, now
        ));
        return updated;
    }

    public ActiveLearningCycle recordReviewProgress(String tenantId, String cycleId, int passedCount,
                                                      String actorId) {
        ActiveLearningCycle cycle = cycleRepository.findById(tenantId, cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found: " + cycleId));
        if (cycle.status() != CycleStatus.AWAITING_REVIEW) {
            throw new IllegalStateException("Cycle is not awaiting review: " + cycle.status());
        }
        Instant now = Instant.now();
        ActiveLearningCycle updated = new ActiveLearningCycle(
                cycle.cycleId(), cycle.tenantId(), cycle.modelEntryId(), cycle.modelVersionId(),
                cycle.strategyType(), CycleStatus.DATASET_UPDATED, cycle.selectedRecordCount(),
                passedCount, cycle.sourceDatasetVersionId(), cycle.targetDatasetVersionId(),
                cycle.retrainingJobId(), cycle.evaluationRunId(), null,
                cycle.startedAt(), null, cycle.cronExpression(), cycle.autoTrigger(),
                cycle.evidenceEventIds(),
                updatedAudit(cycle.audit(), actorId)
        );
        cycleRepository.save(updated);
        return updated;
    }

    public ActiveLearningCycle rejectCycle(String tenantId, String cycleId, String reason, String actorId) {
        return updateStatus(tenantId, cycleId, CycleStatus.REJECTED, null, actorId);
    }

    public ActiveLearningCycle cancelCycle(String tenantId, String cycleId, String actorId) {
        return updateStatus(tenantId, cycleId, CycleStatus.CANCELLED, null, actorId);
    }

    public boolean canTriggerCycle(String tenantId, String modelEntryId) {
        return cycleRepository.findLatestByModel(tenantId, modelEntryId)
                .map(c -> c.status() == CycleStatus.PROMOTED
                        || c.status() == CycleStatus.REJECTED
                        || c.status() == CycleStatus.CANCELLED
                        || c.status() == CycleStatus.FAILED)
                .orElse(true);
    }

    private static boolean isValidTransition(CycleStatus from, CycleStatus to) {
        return switch (from) {
            case SELECTING -> to == CycleStatus.AWAITING_REVIEW || to == CycleStatus.FAILED;
            case AWAITING_REVIEW -> to == CycleStatus.DATASET_UPDATED || to == CycleStatus.CANCELLED || to == CycleStatus.FAILED;
            case DATASET_UPDATED -> to == CycleStatus.RETRAINING || to == CycleStatus.CANCELLED;
            case RETRAINING -> to == CycleStatus.EVALUATING || to == CycleStatus.FAILED || to == CycleStatus.CANCELLED;
            case EVALUATING -> to == CycleStatus.PROMOTED || to == CycleStatus.REJECTED || to == CycleStatus.FAILED;
            case PROMOTED, REJECTED, CANCELLED, FAILED -> false;
        };
    }

    private static AuditMetadata updatedAudit(AuditMetadata existing, String actorId) {
        return new AuditMetadata(existing.createdAt(), existing.createdBy(),
                Instant.now(), actorId, existing.version() + 1);
    }
}

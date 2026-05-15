package ai.datalithix.kanon.activelearning.service;

import ai.datalithix.kanon.activelearning.model.ActiveLearningCycle;
import ai.datalithix.kanon.activelearning.model.CycleStatus;
import ai.datalithix.kanon.common.ActorType;
import ai.datalithix.kanon.evidence.model.EvidenceEvent;
import ai.datalithix.kanon.evidence.service.EvidenceLedger;
import ai.datalithix.kanon.modelregistry.model.ModelLifecycleStage;
import ai.datalithix.kanon.modelregistry.service.EvaluationService;
import ai.datalithix.kanon.modelregistry.service.ModelRegistryService;
import ai.datalithix.kanon.training.model.HyperParameterConfig;
import ai.datalithix.kanon.training.service.TrainingOrchestrationService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ActiveLearningPipelineService {
    private static final Logger log = LoggerFactory.getLogger(ActiveLearningPipelineService.class);

    private final ActiveLearningOrchestrator orchestrator;
    private final ActiveLearningCycleRepository cycleRepository;
    private final TrainingOrchestrationService trainingService;
    private final EvaluationService evaluationService;
    private final ModelRegistryService modelRegistryService;
    private final EvidenceLedger evidenceLedger;

    public ActiveLearningPipelineService(
            ActiveLearningOrchestrator orchestrator,
            ActiveLearningCycleRepository cycleRepository,
            TrainingOrchestrationService trainingService,
            EvaluationService evaluationService,
            ModelRegistryService modelRegistryService,
            EvidenceLedger evidenceLedger
    ) {
        this.orchestrator = orchestrator;
        this.cycleRepository = cycleRepository;
        this.trainingService = trainingService;
        this.evaluationService = evaluationService;
        this.modelRegistryService = modelRegistryService;
        this.evidenceLedger = evidenceLedger;
    }

    public ActiveLearningCycle completeReview(String tenantId, String cycleId, int passedCount,
                                               boolean proceedToRetraining, String computeBackendId,
                                               String actorId) {
        var updated = orchestrator.recordReviewProgress(tenantId, cycleId, passedCount, actorId);
        if (proceedToRetraining) {
            return triggerRetraining(tenantId, cycleId, computeBackendId, actorId);
        }
        return orchestrator.updateStatus(tenantId, cycleId, CycleStatus.REJECTED, null, actorId);
    }

    public ActiveLearningCycle triggerRetraining(String tenantId, String cycleId,
                                                  String computeBackendId, String actorId) {
        var cycle = cycleRepository.findById(tenantId, cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found: " + cycleId));
        if (cycle.status() != CycleStatus.DATASET_UPDATED && cycle.status() != CycleStatus.AWAITING_REVIEW) {
            throw new IllegalStateException("Cycle must be DATASET_UPDATED or AWAITING_REVIEW, was: " + cycle.status());
        }
        if (cycle.targetDatasetVersionId() == null) {
            throw new IllegalStateException("Target dataset version ID is not set for cycle: " + cycleId);
        }
        var modelEntry = modelRegistryService.getModelEntry(tenantId, cycle.modelEntryId());
        if (modelEntry == null) {
            throw new IllegalArgumentException("Model entry not found: " + cycle.modelEntryId());
        }
        var hyperParams = new HyperParameterConfig(
                modelEntry.framework(), null, 10, 32, 0.001, Map.of());
        var latestVersion = modelRegistryService.listVersions(tenantId, cycle.modelEntryId()).stream()
                .filter(v -> v.modelVersionId().equals(cycle.modelVersionId()))
                .findFirst().orElse(null);
        var datasetDefinitionId = latestVersion != null ? latestVersion.datasetDefinitionId() : null;
        var job = trainingService.submitJob(
                tenantId, cycle.targetDatasetVersionId(), datasetDefinitionId,
                computeBackendId, modelEntry.modelName() + "-al-v" + cycle.startedAt().toEpochMilli(),
                hyperParams, actorId);

        var inRetraining = orchestrator.updateStatus(tenantId, cycleId, CycleStatus.RETRAINING, null, actorId);
        var withJobId = new ActiveLearningCycle(
                inRetraining.cycleId(), inRetraining.tenantId(), inRetraining.modelEntryId(),
                inRetraining.modelVersionId(), inRetraining.strategyType(), inRetraining.status(),
                inRetraining.selectedRecordCount(), inRetraining.passedReviewCount(),
                inRetraining.sourceDatasetVersionId(), inRetraining.targetDatasetVersionId(),
                job.trainingJobId(), inRetraining.evaluationRunId(), null,
                inRetraining.startedAt(), null, inRetraining.cronExpression(), inRetraining.autoTrigger(),
                inRetraining.evidenceEventIds(), inRetraining.audit()
        );
        cycleRepository.save(withJobId);
        evidenceLedger.append(new EvidenceEvent(UUID.randomUUID().toString(), tenantId, cycleId,
                "RETRAINING_TRIGGERED", ActorType.SYSTEM, actorId, "active-learning-pipeline",
                null, null,
                Map.of("previousStatus", cycle.status().name(), "modelEntryId", cycle.modelEntryId()),
                Map.of("newStatus", CycleStatus.RETRAINING.name(), "trainingJobId", job.trainingJobId()),
                "Retraining triggered for AL cycle " + cycleId + " with job " + job.trainingJobId(),
                Instant.now()));
        log.info("Triggered retraining job {} for AL cycle {}", job.trainingJobId(), cycleId);
        return withJobId;
    }

    public ActiveLearningCycle completeRetraining(String tenantId, String cycleId, String actorId) {
        var cycle = cycleRepository.findById(tenantId, cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found: " + cycleId));
        if (cycle.status() != CycleStatus.RETRAINING) {
            throw new IllegalStateException("Cycle must be RETRAINING, was: " + cycle.status());
        }
        var modelVersion = modelRegistryService.listVersions(tenantId, cycle.modelEntryId()).stream()
                .filter(v -> v.trainingJobId() != null && v.trainingJobId().equals(cycle.retrainingJobId()))
                .findFirst()
                .orElse(null);
        if (modelVersion == null) {
            throw new IllegalStateException("No model version found for retraining job: " + cycle.retrainingJobId());
        }
        var evaluationRun = evaluationService.evaluate(
                tenantId, modelVersion.modelVersionId(), cycle.sourceDatasetVersionId(), actorId);
        var evaluating = orchestrator.updateStatus(tenantId, cycleId, CycleStatus.EVALUATING, null, actorId);
        var withEvalId = new ActiveLearningCycle(
                evaluating.cycleId(), evaluating.tenantId(), evaluating.modelEntryId(),
                evaluating.modelVersionId(), evaluating.strategyType(), evaluating.status(),
                evaluating.selectedRecordCount(), evaluating.passedReviewCount(),
                evaluating.sourceDatasetVersionId(), evaluating.targetDatasetVersionId(),
                evaluating.retrainingJobId(), evaluationRun.evaluationRunId(), null,
                evaluating.startedAt(), null, evaluating.cronExpression(), evaluating.autoTrigger(),
                evaluating.evidenceEventIds(), evaluating.audit()
        );
        cycleRepository.save(withEvalId);
        if (evaluationRun.passedThreshold()) {
            modelRegistryService.promoteModel(tenantId, modelVersion.modelVersionId(),
                    ModelLifecycleStage.PRODUCTION, actorId);
            var promoted = orchestrator.updateStatus(tenantId, cycleId, CycleStatus.PROMOTED, null, actorId);
            evidenceLedger.append(new EvidenceEvent(UUID.randomUUID().toString(), tenantId, cycleId,
                    "RETRAINING_COMPLETED", ActorType.SYSTEM, actorId, "active-learning-pipeline",
                    null, null,
                    Map.of("modelVersionId", modelVersion.modelVersionId(), "thresholdPassed", true),
                    Map.of("newStatus", CycleStatus.PROMOTED.name()),
                    "Retraining completed and model promoted for AL cycle " + cycleId,
                    Instant.now()));
            return promoted;
        }
        var rejected = orchestrator.updateStatus(tenantId, cycleId, CycleStatus.REJECTED, null, actorId);
        evidenceLedger.append(new EvidenceEvent(UUID.randomUUID().toString(), tenantId, cycleId,
                "RETRAINING_COMPLETED", ActorType.SYSTEM, actorId, "active-learning-pipeline",
                null, null,
                Map.of("modelVersionId", modelVersion.modelVersionId(), "thresholdPassed", false),
                Map.of("newStatus", CycleStatus.REJECTED.name()),
                "Retraining completed but evaluation threshold not met for AL cycle " + cycleId,
                Instant.now()));
        return rejected;
    }

    public boolean canAutoTrigger(String tenantId, String modelEntryId) {
        return orchestrator.canTriggerCycle(tenantId, modelEntryId);
    }
}

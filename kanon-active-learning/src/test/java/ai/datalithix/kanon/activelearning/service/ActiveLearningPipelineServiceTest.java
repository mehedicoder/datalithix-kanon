package ai.datalithix.kanon.activelearning.service;

import ai.datalithix.kanon.activelearning.model.ActiveLearningCycle;
import ai.datalithix.kanon.activelearning.model.CycleStatus;
import ai.datalithix.kanon.activelearning.model.SelectionStrategyType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.evidence.model.EvidenceEvent;
import ai.datalithix.kanon.evidence.service.EvidenceLedger;
import ai.datalithix.kanon.modelregistry.model.EvaluationMetric;
import ai.datalithix.kanon.modelregistry.model.EvaluationRun;
import ai.datalithix.kanon.modelregistry.model.ModelArtifact;
import ai.datalithix.kanon.modelregistry.model.ModelEntry;
import ai.datalithix.kanon.modelregistry.model.ModelLifecycleStage;
import ai.datalithix.kanon.modelregistry.model.ModelVersion;
import ai.datalithix.kanon.modelregistry.service.EvaluationService;
import ai.datalithix.kanon.modelregistry.service.ModelRegistryService;
import ai.datalithix.kanon.training.model.HyperParameterConfig;
import ai.datalithix.kanon.training.model.TrainingJob;
import ai.datalithix.kanon.training.service.TrainingOrchestrationService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActiveLearningPipelineServiceTest {
    private final InMemoryActiveLearningCycleRepository cycleRepo = new InMemoryActiveLearningCycleRepository();
    private final CapturingEvidenceLedger ledger = new CapturingEvidenceLedger();
    private final StubOrchestrator orchestrator = new StubOrchestrator();
    private final StubTrainingService trainingService = new StubTrainingService();
    private final StubEvaluationService evaluationService = new StubEvaluationService();
    private final StubModelRegistryService modelRegistryService = new StubModelRegistryService();

    private final ActiveLearningPipelineService pipeline = new ActiveLearningPipelineService(
            orchestrator, cycleRepo, trainingService, evaluationService, modelRegistryService, ledger);

    @Test
    void completeReviewProceedsToRetraining() {
        var cycle = createAwaitingCycle();
        var result = pipeline.completeReview("tenant-1", cycle.cycleId(), 5, true, "k8s", "user-1");
        assertEquals(CycleStatus.RETRAINING, result.status());
        assertNotNull(result.retrainingJobId());
    }

    @Test
    void completeReviewRejectsWhenNotProceeding() {
        var cycle = createAwaitingCycle();
        var result = pipeline.completeReview("tenant-1", cycle.cycleId(), 0, false, null, "user-1");
        assertEquals(CycleStatus.REJECTED, result.status());
    }

    @Test
    void triggerRetrainingRequiresValidStatus() {
        var cycle = createCycle(CycleStatus.RETRAINING);
        cycleRepo.save(cycle);
        assertThrows(IllegalStateException.class,
                () -> pipeline.triggerRetraining("tenant-1", cycle.cycleId(), "k8s", "user-1"));
    }

    @Test
    void completeRetrainingPromotesOnPass() {
        var cycle = createCycle(CycleStatus.RETRAINING);
        cycleRepo.save(new ActiveLearningCycle(
                cycle.cycleId(), cycle.tenantId(), cycle.modelEntryId(), cycle.modelVersionId(),
                cycle.strategyType(), CycleStatus.RETRAINING, cycle.selectedRecordCount(),
                cycle.passedReviewCount(), cycle.sourceDatasetVersionId(), cycle.targetDatasetVersionId(),
                "tj-1", null, null, cycle.startedAt(), null, null, true,
                cycle.evidenceEventIds(), cycle.audit()));

        evaluationService.evaluationPasses = true;
        var result = pipeline.completeRetraining("tenant-1", cycle.cycleId(), "user-1");
        assertEquals(CycleStatus.PROMOTED, result.status());
    }

    @Test
    void completeRetrainingRejectsOnFail() {
        var cycle = createCycle(CycleStatus.RETRAINING);
        cycleRepo.save(new ActiveLearningCycle(
                cycle.cycleId(), cycle.tenantId(), cycle.modelEntryId(), cycle.modelVersionId(),
                cycle.strategyType(), CycleStatus.RETRAINING, cycle.selectedRecordCount(),
                cycle.passedReviewCount(), cycle.sourceDatasetVersionId(), cycle.targetDatasetVersionId(),
                "tj-1", null, null, cycle.startedAt(), null, null, true,
                cycle.evidenceEventIds(), cycle.audit()));

        evaluationService.evaluationPasses = false;
        var result = pipeline.completeRetraining("tenant-1", cycle.cycleId(), "user-1");
        assertEquals(CycleStatus.REJECTED, result.status());
    }

    @Test
    void canAutoTriggerDelegatesToOrchestrator() {
        orchestrator.canTrigger = true;
        assertTrue(pipeline.canAutoTrigger("tenant-1", "me-1"));
        orchestrator.canTrigger = false;
        assertFalse(pipeline.canAutoTrigger("tenant-1", "me-1"));
    }

    private ActiveLearningCycle createAwaitingCycle() {
        var cycle = createCycle(CycleStatus.AWAITING_REVIEW);
        cycleRepo.save(new ActiveLearningCycle(
                cycle.cycleId(), cycle.tenantId(), cycle.modelEntryId(), cycle.modelVersionId(),
                cycle.strategyType(), CycleStatus.AWAITING_REVIEW, 10, 0,
                cycle.sourceDatasetVersionId(), "dsv-2", null, null, null,
                cycle.startedAt(), null, null, true, cycle.evidenceEventIds(), cycle.audit()));
        return cycleRepo.findById("tenant-1", cycle.cycleId()).orElseThrow();
    }

    private static ActiveLearningCycle createCycle(CycleStatus status) {
        var audit = new AuditMetadata(Instant.now(), "creator", Instant.now(), "creator", 1);
        return new ActiveLearningCycle("cycle-1", "tenant-1", "me-1", "mv-1",
                SelectionStrategyType.UNCERTAINTY_SAMPLING, status, 10, 0,
                "dsv-1", null, null, null, null,
                Instant.now(), null, null, true, List.of(), audit);
    }

    private static class StubOrchestrator extends ActiveLearningOrchestrator {
        boolean canTrigger = true;

        StubOrchestrator() {
            super(null, null, java.util.Collections.emptyList());
        }

        @Override
        public ActiveLearningCycle recordReviewProgress(String tenantId, String cycleId,
                                                         int passedCount, String actorId) {
            var cycle = new ActiveLearningCycle(cycleId, tenantId, "me-1", "mv-1",
                    SelectionStrategyType.UNCERTAINTY_SAMPLING, CycleStatus.DATASET_UPDATED,
                    10, passedCount, "dsv-1", "dsv-2", null, null, null,
                    Instant.now(), null, null, true, List.of(),
                    new AuditMetadata(Instant.now(), actorId, Instant.now(), actorId, 1));
            return cycle;
        }

        @Override
        public ActiveLearningCycle updateStatus(String tenantId, String cycleId,
                                                 CycleStatus status, String targetDatasetVersionId, String actorId) {
            var audit = new AuditMetadata(Instant.now(), actorId, Instant.now(), actorId, 1);
            return new ActiveLearningCycle(cycleId, tenantId, "me-1", "mv-1",
                    SelectionStrategyType.UNCERTAINTY_SAMPLING, status, 10, 5,
                    "dsv-1", targetDatasetVersionId, "tj-1", null, null,
                    Instant.now(), Instant.now(), null, true, List.of(), audit);
        }

        @Override
        public boolean canTriggerCycle(String tenantId, String modelEntryId) {
            return canTrigger;
        }
    }

    private static class StubTrainingService implements TrainingOrchestrationService {
        @Override
        public TrainingJob submitJob(String tenantId, String datasetVersionId, String datasetDefinitionId,
                                      String computeBackendId, String jobName,
                                      HyperParameterConfig hyperParams, String actorId) {
            var audit = new AuditMetadata(Instant.now(), actorId, Instant.now(), actorId, 1);
            var hpc = new HyperParameterConfig("pytorch", null, 10, 32, 0.001, java.util.Map.of());
            return new TrainingJob("tj-1", tenantId, datasetVersionId, datasetDefinitionId,
                    computeBackendId, jobName, hpc,
                    ai.datalithix.kanon.training.model.TrainingJobStatus.QUEUED,
                    Instant.now(), null, null, null, null, null, null,
                    java.util.Collections.emptyList(), 0L, null,
                    java.util.Collections.emptyList(), audit);
        }

        @Override
        public TrainingJob cancelJob(String tenantId, String trainingJobId) { return null; }

        @Override
        public TrainingJob updateJobStatus(String tenantId, String trainingJobId,
                                           ai.datalithix.kanon.training.model.TrainingJobStatus newStatus,
                                           String actorId) { return null; }

        @Override
        public TrainingJob getJobStatus(String tenantId, String trainingJobId) { return null; }

        @Override
        public List<TrainingJob> listJobs(String tenantId) { return List.of(); }

        @Override
        public List<TrainingJob> listJobsByDataset(String tenantId, String datasetVersionId) { return List.of(); }

        @Override
        public ai.datalithix.kanon.training.model.ComputeBackend registerBackend(
                ai.datalithix.kanon.training.model.ComputeBackend backend) { return backend; }

        @Override
        public boolean healthCheckBackend(String tenantId, String backendId) { return true; }
    }

    private static class StubEvaluationService implements EvaluationService {
        boolean evaluationPasses = true;

        @Override
        public EvaluationRun evaluate(String tenantId, String modelVersionId,
                                       String testDatasetVersionId, String actorId) {
            return new EvaluationRun("er-1", modelVersionId, "me-1", tenantId,
                    testDatasetVersionId,
                    List.of(new EvaluationMetric("accuracy", 0.95, "acc")),
                    Map.of(), null, null, evaluationPasses ? "PASSED" : "FAILED",
                    null, Instant.now(), Instant.now(), evaluationPasses, List.of(),
                    new AuditMetadata(Instant.now(), actorId, Instant.now(), actorId, 1));
        }

        @Override
        public EvaluationRun evaluateWithThreshold(String tenantId, String modelVersionId,
                                                    String testDatasetVersionId, double minThreshold, String actorId) {
            return evaluate(tenantId, modelVersionId, testDatasetVersionId, actorId);
        }

        @Override
        public List<EvaluationRun> getEvaluationHistory(String tenantId, String modelVersionId) {
            return List.of();
        }

        @Override
        public ai.datalithix.kanon.modelregistry.model.ModelVersion compareVersions(
                String tenantId, String modelEntryId, int versionA, int versionB) {
            return null;
        }

        @Override
        public List<EvaluationMetric> computeDefaultMetrics(
                String tenantId, String modelVersionId, String testDatasetVersionId) {
            return List.of();
        }
    }

    private static class StubModelRegistryService implements ModelRegistryService {
        @Override
        public ModelEntry registerModel(String tenantId, String modelName, String framework, String taskType,
                                         String domainType, String artifactUri, String trainingJobId,
                                         String datasetVersionId, String actorId) {
            return null;
        }

        @Override
        public ModelEntry getModelEntry(String tenantId, String modelEntryId) {
            return new ModelEntry("me-1", tenantId, "test-model", "desc", "pytorch",
                    null, null, java.util.Collections.emptySet(), 1, null, null,
                    true, java.util.Collections.emptyList(),
                    new AuditMetadata(Instant.now(), "creator", Instant.now(), "creator", 1));
        }

        @Override
        public ModelVersion getModelVersion(String tenantId, String modelVersionId) {
            return listVersions(tenantId, "me-1").getFirst();
        }

        @Override
        public List<ModelVersion> listVersions(String tenantId, String modelEntryId) {
            var audit = new AuditMetadata(Instant.now(), "creator", Instant.now(), "creator", 1);
            var artifact = new ModelArtifact("s3://bucket/model.pt", "pytorch", 0, null, "S3");
            return List.of(new ModelVersion("mv-1", "me-1", "tenant-1", 1, "tj-1",
                    "dsv-1", null, artifact, null, Set.of(),
                    ModelLifecycleStage.DEVELOPMENT, null, null,
                    List.of(), List.of(), List.of(), audit));
        }

        @Override
        public ModelVersion promoteModel(String tenantId, String modelVersionId,
                                          ModelLifecycleStage targetStage, String actorId) {
            return listVersions(tenantId, "me-1").getFirst();
        }

        @Override
        public ModelVersion rollbackModel(String tenantId, String modelEntryId,
                                           int targetVersionNumber, String actorId) {
            return null;
        }

        @Override
        public List<ModelEntry> listModels(String tenantId) { return List.of(); }

        @Override
        public List<ModelVersion> listModelsByStage(String tenantId, ModelLifecycleStage stage) { return List.of(); }
    }

    private static class CapturingEvidenceLedger implements EvidenceLedger {
        @Override public void append(EvidenceEvent event) {}
    }
}

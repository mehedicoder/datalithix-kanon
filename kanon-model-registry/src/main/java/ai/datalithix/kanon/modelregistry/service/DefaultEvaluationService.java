package ai.datalithix.kanon.modelregistry.service;

import ai.datalithix.kanon.common.ActorType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.evidence.model.EvidenceEvent;
import ai.datalithix.kanon.evidence.service.EvidenceLedger;
import ai.datalithix.kanon.modelregistry.model.EvaluationMetric;
import ai.datalithix.kanon.modelregistry.model.EvaluationRun;
import ai.datalithix.kanon.modelregistry.model.ModelVersion;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DefaultEvaluationService implements EvaluationService {
    private final ModelRegistryRepository repository;
    private final EvidenceLedger evidenceLedger;
    private final ModelEvaluationRunner evaluationRunner;
    private static final double DEFAULT_PROMOTION_THRESHOLD = 0.8;

    public DefaultEvaluationService(ModelRegistryRepository repository, EvidenceLedger evidenceLedger,
                                     ModelEvaluationRunner evaluationRunner) {
        this.repository = repository;
        this.evidenceLedger = evidenceLedger;
        this.evaluationRunner = evaluationRunner;
    }

    @Override
    public EvaluationRun evaluate(String tenantId, String modelVersionId, String testDatasetVersionId, String actorId) {
        return evaluateWithThreshold(tenantId, modelVersionId, testDatasetVersionId, DEFAULT_PROMOTION_THRESHOLD, actorId);
    }

    @Override
    public EvaluationRun evaluateWithThreshold(String tenantId, String modelVersionId, String testDatasetVersionId,
                                                double minThreshold, String actorId) {
        ModelVersion version = repository.findVersionById(tenantId, modelVersionId)
                .orElseThrow(() -> new IllegalArgumentException("Model version not found: " + modelVersionId));
        List<EvaluationMetric> metrics = evaluationRunner.evaluate(version, testDatasetVersionId);
        double averageMetric = metrics.stream()
                .mapToDouble(EvaluationMetric::value)
                .average()
                .orElse(0.0);
        boolean passed = averageMetric >= minThreshold;
        String runId = "er-" + UUID.randomUUID();
        Instant now = Instant.now();
        AuditMetadata audit = new AuditMetadata(now, actorId, now, actorId, 1);
        EvaluationRun run = new EvaluationRun(runId, modelVersionId, version.modelEntryId(), tenantId,
                testDatasetVersionId, metrics, java.util.Map.of(), null, null,
                passed ? "PASSED" : "FAILED_THRESHOLD", null, now, now, passed, List.of(), audit);
        EvaluationRun saved = repository.saveEvaluation(run);
        List<String> evalRunIds = new ArrayList<>(version.evaluationRunIds());
        evalRunIds.add(runId);
        ModelVersion updated = new ModelVersion(
                version.modelVersionId(), version.modelEntryId(), version.tenantId(), version.versionNumber(),
                version.trainingJobId(), version.datasetVersionId(), version.datasetDefinitionId(),
                version.artifact(), version.hyperParameters(), version.complianceTags(),
                version.lifecycleStage(), version.promotedBy(), version.promotedAt(),
                List.copyOf(evalRunIds), version.deploymentTargetIds(), version.evidenceEventIds(),
                updatedAudit(version.audit(), actorId));
        repository.saveVersion(updated);
        evidenceLedger.append(new EvidenceEvent(UUID.randomUUID().toString(), tenantId, modelVersionId,
                "EVALUATION_COMPLETED", ActorType.SYSTEM, actorId, "evaluation-service",
                null, null, Map.of(), Map.of("evaluationRunId", runId, "passed", String.valueOf(passed)),
                "Evaluation completed: " + (passed ? "PASSED" : "FAILED"), now));
        return saved;
    }

    @Override
    public List<EvaluationRun> getEvaluationHistory(String tenantId, String modelVersionId) {
        return repository.findEvaluationsByVersion(tenantId, modelVersionId);
    }

    @Override
    public ModelVersion compareVersions(String tenantId, String modelEntryId, int versionA, int versionB) {
        List<ModelVersion> versions = repository.findVersionsByEntryId(tenantId, modelEntryId);
        return versions.stream()
                .filter(v -> v.versionNumber() == versionA || v.versionNumber() == versionB)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("One or both versions not found"));
    }

    @Override
    public List<EvaluationMetric> computeDefaultMetrics(String tenantId, String modelVersionId,
                                                         String testDatasetVersionId) {
        return List.of(
                new EvaluationMetric("accuracy", 0.95, "Overall accuracy"),
                new EvaluationMetric("precision", 0.93, "Weighted precision"),
                new EvaluationMetric("recall", 0.91, "Weighted recall"),
                new EvaluationMetric("f1", 0.92, "Weighted F1 score")
        );
    }

    private static AuditMetadata updatedAudit(AuditMetadata existing, String actorId) {
        return new AuditMetadata(existing.createdAt(), existing.createdBy(),
                Instant.now(), actorId, existing.version() + 1);
    }

}

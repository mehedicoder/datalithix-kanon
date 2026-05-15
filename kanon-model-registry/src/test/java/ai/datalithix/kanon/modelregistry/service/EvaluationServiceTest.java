package ai.datalithix.kanon.modelregistry.service;

import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.evidence.model.EvidenceEvent;
import ai.datalithix.kanon.evidence.service.EvidenceLedger;
import ai.datalithix.kanon.modelregistry.model.EvaluationMetric;
import ai.datalithix.kanon.modelregistry.model.EvaluationRun;
import ai.datalithix.kanon.modelregistry.model.ModelArtifact;
import ai.datalithix.kanon.modelregistry.model.ModelLifecycleStage;
import ai.datalithix.kanon.modelregistry.model.ModelVersion;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EvaluationServiceTest {
    private final InMemoryModelRegistryRepository repository = new InMemoryModelRegistryRepository();
    private final CapturingEvidenceLedger ledger = new CapturingEvidenceLedger();
    private final DefaultEvaluationService service = new DefaultEvaluationService(repository, ledger,
            new StubEvaluationRunner());

    @Test
    void evaluatesModelWithDefaultMetrics() {
        ModelVersion version = setupVersion();

        EvaluationRun run = service.evaluate("tenant-1", version.modelVersionId(), "test-dsv-1", "user-1");

        assertNotNull(run);
        assertEquals("PASSED", run.status());
        assertFalse(run.metrics().isEmpty());
    }

    @Test
    void evaluationContainsExpectedMetrics() {
        ModelVersion version = setupVersion();

        EvaluationRun run = service.evaluate("tenant-1", version.modelVersionId(), "test-dsv-1", "user-1");

        List<String> metricNames = run.metrics().stream().map(EvaluationMetric::metricName).toList();
        assertTrue(metricNames.containsAll(List.of("accuracy", "precision", "recall", "f1")));
    }

    @Test
    void failsEvaluationBelowThreshold() {
        ModelVersion version = setupVersion();

        EvaluationRun run = service.evaluateWithThreshold("tenant-1", version.modelVersionId(),
                "test-dsv-1", 0.99, "user-1");

        assertEquals("FAILED_THRESHOLD", run.status());
    }

    @Test
    void passesEvaluationAboveThreshold() {
        ModelVersion version = setupVersion();

        EvaluationRun run = service.evaluateWithThreshold("tenant-1", version.modelVersionId(),
                "test-dsv-1", 0.5, "user-1");

        assertEquals("PASSED", run.status());
    }

    @Test
    void returnsEvaluationHistory() {
        ModelVersion version = setupVersion();
        service.evaluate("tenant-1", version.modelVersionId(), "test-dsv-1", "user-1");
        service.evaluate("tenant-1", version.modelVersionId(), "test-dsv-1", "user-1");

        List<EvaluationRun> history = service.getEvaluationHistory("tenant-1", version.modelVersionId());

        assertEquals(2, history.size());
    }

    @Test
    void rejectsEvaluationForNonexistentModel() {
        assertThrows(IllegalArgumentException.class, () ->
                service.evaluate("tenant-1", "nonexistent", "test-dsv-1", "user-1"));
    }

    @Test
    void computesDefaultMetrics() {
        ModelVersion version = setupVersion();

        List<EvaluationMetric> metrics = service.computeDefaultMetrics(
                "tenant-1", version.modelVersionId(), "test-dsv-1");

        assertEquals(4, metrics.size());
        assertTrue(metrics.stream().allMatch(m -> m.value() > 0));
    }

    private ModelVersion setupVersion() {
        Instant now = Instant.now();
        AuditMetadata audit = new AuditMetadata(now, "creator", now, "creator", 1);
        ModelArtifact artifact = new ModelArtifact("s3://models/test/v1/model.pt", "pytorch", 0, null, "S3");
        ModelVersion version = new ModelVersion("mv-1", "me-1", "tenant-1", 1, "tj-1",
                "dsv-1", null, artifact, null, java.util.Set.of(),
                ModelLifecycleStage.DEVELOPMENT, null, null, List.of(), List.of(), List.of(), audit);
        repository.saveVersion(version);
        return version;
    }

    private static class CapturingEvidenceLedger implements EvidenceLedger {
        @Override public void append(EvidenceEvent event) {}
    }

    private static class StubEvaluationRunner extends ModelEvaluationRunner {
        @Override
        public java.util.List<EvaluationMetric> evaluate(ModelVersion version, String testDatasetVersionId) {
            return List.of(
                    new EvaluationMetric("accuracy", 0.95, "Overall accuracy"),
                    new EvaluationMetric("precision", 0.93, "Weighted precision"),
                    new EvaluationMetric("recall", 0.91, "Weighted recall"),
                    new EvaluationMetric("f1", 0.92, "Weighted F1 score")
            );
        }
    }
}

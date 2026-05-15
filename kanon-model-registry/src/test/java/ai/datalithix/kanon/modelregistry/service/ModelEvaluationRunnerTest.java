package ai.datalithix.kanon.modelregistry.service;

import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.modelregistry.model.EvaluationMetric;
import ai.datalithix.kanon.modelregistry.model.ModelArtifact;
import ai.datalithix.kanon.modelregistry.model.ModelLifecycleStage;
import ai.datalithix.kanon.modelregistry.model.ModelVersion;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelEvaluationRunnerTest {

    private final ModelEvaluationRunner runner = new ModelEvaluationRunner();

    @Test
    void fallbackMetricsReturnedForNonHttpArtifactUri() {
        var version = createVersion("s3://bucket/model.pt", null);
        var metrics = runner.evaluate(version, "test-dsv-1");
        assertFalse(metrics.isEmpty());
        assertTrue(metrics.stream().allMatch(m -> m.value() > 0));
    }

    @Test
    void fallbackMetricsIncludeAccuracyPrecisionRecallF1() {
        var version = createVersion("s3://bucket/model.pt", null);
        var metrics = runner.evaluate(version, "test-dsv-1");
        var names = metrics.stream().map(EvaluationMetric::metricName).toList();
        assertTrue(names.containsAll(List.of("accuracy", "precision", "recall", "f1")));
    }

    @Test
    void fallbackMetricsRespectHyperparameters() {
        var version = createVersion("s3://bucket/model.pt",
                new ai.datalithix.kanon.training.model.HyperParameterConfig(
                        "pytorch", null, 10, 32, 0.02, java.util.Map.of()));
        var metrics = runner.evaluate(version, "test-dsv-1");
        var accuracy = metrics.stream()
                .filter(m -> m.metricName().equals("accuracy"))
                .findFirst().orElseThrow();
        assertEquals(0.9, accuracy.value(), 0.01);
    }

    @Test
    void remoteEvaluationFailsGracefully() {
        var version = createVersion("http://localhost:1/nonexistent/evaluate", null);
        var metrics = runner.evaluate(version, "test-dsv-1");
        assertFalse(metrics.isEmpty());
    }

    private static ModelVersion createVersion(String artifactUri,
                                               ai.datalithix.kanon.training.model.HyperParameterConfig hyperParams) {
        var audit = new AuditMetadata(Instant.now(), "creator", Instant.now(), "creator", 1);
        var artifact = new ModelArtifact(artifactUri, "pytorch", 0, null, "S3");
        return new ModelVersion("mv-1", "me-1", "tenant-1", 1, "tj-1",
                "dsv-1", null, artifact, hyperParams, Set.of(),
                ModelLifecycleStage.DEVELOPMENT, null, null,
                List.of(), List.of(), List.of(), audit);
    }
}

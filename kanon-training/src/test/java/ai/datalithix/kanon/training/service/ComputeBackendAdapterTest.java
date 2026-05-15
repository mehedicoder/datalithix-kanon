package ai.datalithix.kanon.training.service;

import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.training.model.ComputeBackend;
import ai.datalithix.kanon.training.model.ComputeBackendType;
import ai.datalithix.kanon.training.model.HyperParameterConfig;
import ai.datalithix.kanon.training.model.TrainingJob;
import ai.datalithix.kanon.training.model.TrainingJobStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComputeBackendAdapterTest {

    private final HyperParameterConfig hyperParams = new HyperParameterConfig(
            "pytorch", "bert-base", 3, 16, 0.001, Map.of());
    private final AuditMetadata audit = new AuditMetadata(Instant.now(), "tester",
            Instant.now(), "tester", 1);
    private final TrainingJob sampleJob = new TrainingJob("tj-1", "tenant-1", "dv-1",
            "dd-1", "cb-1", "model-x", hyperParams,
            TrainingJobStatus.QUEUED, Instant.now(), null, null, null,
            null, null, null, List.of(), 0L, null, List.of(), audit);

    @Test
    void localGpu_returnsCorrectType() {
        var adapter = new LocalGpuComputeBackend();
        assertEquals(ComputeBackendType.LOCAL_GPU, adapter.supportedBackendType());
    }

    @Test
    void localGpu_submitAndCheck() {
        var adapter = new LocalGpuComputeBackend();
        var backend = new ComputeBackend("b-1", "tenant-1", ComputeBackendType.LOCAL_GPU,
                "local", null, null, Map.of(), true, true, null, null);
        String jobId = adapter.submitJob(sampleJob, backend, "file:///data");
        assertNotNull(jobId);
        assertTrue(jobId.startsWith("local-gpu-"));
        var result = adapter.checkStatus(jobId, backend);
        assertEquals("COMPLETED", result.status());
        assertTrue(adapter.healthCheck(backend));
        assertTrue(adapter.cancelJob(jobId, backend));
    }

    @Test
    void kubernetes_returnsCorrectType() {
        var adapter = new KubernetesComputeBackend();
        assertEquals(ComputeBackendType.KUBERNETES_BATCH, adapter.supportedBackendType());
    }

    @Test
    void kubernetes_submitAndHealthCheck() {
        var adapter = new KubernetesComputeBackend();
        var healthyBackend = new ComputeBackend("b-2", "tenant-1", ComputeBackendType.KUBERNETES_BATCH,
                "k8s", null, null, Map.of("kubeconfig", "/path/to/config"), true, true, null, null);
        var unhealthyBackend = new ComputeBackend("b-3", "tenant-1", ComputeBackendType.KUBERNETES_BATCH,
                "k8s-bad", null, null, Map.of(), true, true, null, null);
        String jobId = adapter.submitJob(sampleJob, healthyBackend, "s3://data");
        assertTrue(jobId.startsWith("k8s-job-"));
        assertTrue(adapter.healthCheck(healthyBackend));
        assertFalse(adapter.healthCheck(unhealthyBackend));
    }

    @Test
    void vertexAi_returnsCorrectType() {
        var adapter = new VertexAiComputeBackend();
        assertEquals(ComputeBackendType.VERTEX_AI, adapter.supportedBackendType());
    }

    @Test
    void vertexAi_submitAndHealthCheck() {
        var adapter = new VertexAiComputeBackend();
        var healthyBackend = new ComputeBackend("b-4", "tenant-1", ComputeBackendType.VERTEX_AI,
                "vertex", null, null, Map.of("projectId", "my-project"), true, true, null, null);
        var unhealthyBackend = new ComputeBackend("b-5", "tenant-1", ComputeBackendType.VERTEX_AI,
                "vertex-bad", null, null, Map.of(), true, true, null, null);
        String jobId = adapter.submitJob(sampleJob, healthyBackend, "gs://data");
        assertTrue(jobId.startsWith("vertex-ai-"));
        assertTrue(adapter.healthCheck(healthyBackend));
        assertFalse(adapter.healthCheck(unhealthyBackend));
    }

    @Test
    void sageMaker_returnsCorrectType() {
        var adapter = new SageMakerComputeBackend();
        assertEquals(ComputeBackendType.SAGEMAKER, adapter.supportedBackendType());
    }

    @Test
    void sageMaker_submitAndHealthCheck() {
        var adapter = new SageMakerComputeBackend();
        var healthyBackend = new ComputeBackend("b-6", "tenant-1", ComputeBackendType.SAGEMAKER,
                "sagemaker", null, null, Map.of("roleArn", "arn:aws:iam::123:role/xyz"), true, true, null, null);
        var unhealthyBackend = new ComputeBackend("b-7", "tenant-1", ComputeBackendType.SAGEMAKER,
                "sagemaker-bad", null, null, Map.of(), true, true, null, null);
        String jobId = adapter.submitJob(sampleJob, healthyBackend, "s3://data");
        assertTrue(jobId.startsWith("sagemaker-"));
        assertTrue(adapter.healthCheck(healthyBackend));
        assertFalse(adapter.healthCheck(unhealthyBackend));
    }

    @Test
    void azureMl_returnsCorrectType() {
        var adapter = new AzureMlComputeBackend();
        assertEquals(ComputeBackendType.AZURE_ML, adapter.supportedBackendType());
    }

    @Test
    void azureMl_submitAndHealthCheck() {
        var adapter = new AzureMlComputeBackend();
        var healthyBackend = new ComputeBackend("b-8", "tenant-1", ComputeBackendType.AZURE_ML,
                "azure", null, null, Map.of("workspaceId", "ws-123"), true, true, null, null);
        var unhealthyBackend = new ComputeBackend("b-9", "tenant-1", ComputeBackendType.AZURE_ML,
                "azure-bad", null, null, Map.of(), true, true, null, null);
        String jobId = adapter.submitJob(sampleJob, healthyBackend, "azureml://data");
        assertTrue(jobId.startsWith("azure-ml-"));
        assertTrue(adapter.healthCheck(healthyBackend));
        assertFalse(adapter.healthCheck(unhealthyBackend));
    }
}

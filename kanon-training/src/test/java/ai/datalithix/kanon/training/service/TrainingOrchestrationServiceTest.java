package ai.datalithix.kanon.training.service;

import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.dataset.model.DatasetDefinition;
import ai.datalithix.kanon.dataset.model.DatasetVersion;
import ai.datalithix.kanon.dataset.model.ExportFormat;
import ai.datalithix.kanon.dataset.model.SplitStrategy;
import ai.datalithix.kanon.dataset.service.DatasetRepository;
import ai.datalithix.kanon.dataset.service.InMemoryDatasetRepository;
import ai.datalithix.kanon.evidence.model.EvidenceEvent;
import ai.datalithix.kanon.evidence.service.EvidenceLedger;
import ai.datalithix.kanon.training.model.ComputeBackend;
import ai.datalithix.kanon.training.model.ComputeBackendType;
import ai.datalithix.kanon.training.model.HyperParameterConfig;
import ai.datalithix.kanon.training.model.TrainingJob;
import ai.datalithix.kanon.training.model.TrainingJobStatus;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrainingOrchestrationServiceTest {
    private final InMemoryTrainingJobRepository jobRepository = new InMemoryTrainingJobRepository();
    private final InMemoryDatasetRepository datasetRepository = new InMemoryDatasetRepository();
    private final CapturingEvidenceLedger ledger = new CapturingEvidenceLedger();
    private final FakeComputeBackendAdapter fakeBackend = new FakeComputeBackendAdapter();
    private final DefaultTrainingOrchestrationService service = new DefaultTrainingOrchestrationService(
            jobRepository, datasetRepository, ledger, List.of(fakeBackend));

    @Test
    void submitsTrainingJob() {
        setupDataset();
        setupBackend("backend-1");

        TrainingJob job = service.submitJob("tenant-1", "v-1", "def-1",
                "backend-1", "test-model", hyperParams(), "user-1");

        assertNotNull(job);
        assertNotNull(job.trainingJobId());
        assertEquals(TrainingJobStatus.QUEUED, job.status());
        assertEquals("backend-1", job.computeBackendId());
    }

    @Test
    void rejectsSubmitForNonexistentDataset() {
        setupBackend("backend-1");

        assertThrows(IllegalArgumentException.class, () ->
                service.submitJob("tenant-1", "nonexistent", "def-1",
                        "backend-1", "test-model", hyperParams(), "user-1"));
    }

    @Test
    void rejectsSubmitForNonexistentBackend() {
        setupDataset();

        assertThrows(IllegalArgumentException.class, () ->
                service.submitJob("tenant-1", "v-1", "def-1",
                        "nonexistent", "test-model", hyperParams(), "user-1"));
    }

    @Test
    void cancelsRunningJob() {
        setupDataset();
        setupBackend("backend-1");
        TrainingJob job = service.submitJob("tenant-1", "v-1", "def-1",
                "backend-1", "test-model", hyperParams(), "user-1");

        TrainingJob cancelled = service.cancelJob("tenant-1", job.trainingJobId());

        assertEquals(TrainingJobStatus.CANCELLED, cancelled.status());
    }

    @Test
    void cannotCancelCompletedJob() {
        setupDataset();
        setupBackend("backend-1");
        TrainingJob job = service.submitJob("tenant-1", "v-1", "def-1",
                "backend-1", "test-model", hyperParams(), "user-1");
        jobRepository.save(completedJob(job));

        assertThrows(IllegalStateException.class, () ->
                service.cancelJob("tenant-1", job.trainingJobId()));
    }

    @Test
    void getsJobStatus() {
        setupDataset();
        setupBackend("backend-1");
        TrainingJob job = service.submitJob("tenant-1", "v-1", "def-1",
                "backend-1", "test-model", hyperParams(), "user-1");

        TrainingJob found = service.getJobStatus("tenant-1", job.trainingJobId());

        assertEquals(job.trainingJobId(), found.trainingJobId());
    }

    @Test
    void listsJobsByTenant() {
        setupDataset();
        setupBackend("backend-1");
        service.submitJob("tenant-1", "v-1", "def-1", "backend-1", "m1", hyperParams(), "user-1");
        service.submitJob("tenant-1", "v-1", "def-1", "backend-1", "m2", hyperParams(), "user-1");

        List<TrainingJob> jobs = service.listJobs("tenant-1");

        assertEquals(2, jobs.size());
    }

    @Test
    void enforcesTenantIsolationOnJobListing() {
        setupDataset();
        setupBackend("backend-1");
        service.submitJob("tenant-1", "v-1", "def-1", "backend-1", "m1", hyperParams(), "user-1");

        List<TrainingJob> jobs = service.listJobs("other-tenant");

        assertTrue(jobs.isEmpty());
    }

    @Test
    void doesNotSubmitJobForUnsupportedBackendType() {
        setupDataset();
        jobRepository.saveBackend(new ComputeBackend("unknown-bk", "tenant-1", ComputeBackendType.KUBERNETES_BATCH,
                "k8s", "http://k8s", null, java.util.Map.of(), true, true, null, null));

        assertThrows(IllegalArgumentException.class, () ->
                service.submitJob("tenant-1", "v-1", "def-1",
                        "unknown-bk", "test-model", hyperParams(), "user-1"));
    }

    private void setupDataset() {
        Instant now = Instant.now();
        AuditMetadata audit = new AuditMetadata(now, "creator", now, "creator", 1);
        DatasetDefinition def = new DatasetDefinition("def-1", "tenant-1", "test", "test",
                "ACCOUNTING", Set.of("ann-1"), null, SplitStrategy.RANDOM, 0.8, 0.1, 0.1,
                List.of(ExportFormat.JSONL), "EU", true, 0, audit);
        datasetRepository.save(def);
        DatasetVersion version = new DatasetVersion("v-1", "def-1", "tenant-1", 1, null,
                SplitStrategy.RANDOM, List.of(), java.util.Map.of(), java.util.Map.of(),
                10, now, "creator", "CURATED", null, null, null, List.of(), audit);
        datasetRepository.saveVersion(version);
    }

    private void setupBackend(String backendId) {
        ComputeBackend backend = new ComputeBackend(backendId, "tenant-1", ComputeBackendType.LOCAL_GPU,
                "local-gpu", "http://localhost", null, java.util.Map.of(), true, true, null, null);
        jobRepository.saveBackend(backend);
    }

    private static HyperParameterConfig hyperParams() {
        return new HyperParameterConfig("pytorch", "bert-base", 3, 16, 0.001, java.util.Map.of());
    }

    private static TrainingJob completedJob(TrainingJob job) {
        return new TrainingJob(
                job.trainingJobId(), job.tenantId(), job.datasetVersionId(), job.datasetDefinitionId(),
                job.computeBackendId(), job.modelName(), job.hyperParameters(), TrainingJobStatus.COMPLETED,
                job.requestedAt(), Instant.now(), Instant.now(), null, null,
                job.checkpointUri(), "s3://models/test", job.metricsHistory(),
                100L, job.externalJobId(), job.evidenceEventIds(), job.audit());
    }

    private static class CapturingEvidenceLedger implements EvidenceLedger {
        private final List<Object> events = new java.util.ArrayList<>();
        @Override public void append(EvidenceEvent event) { events.add(event); }
        List<Object> events() { return events; }
    }

    private static class FakeComputeBackendAdapter implements ComputeBackendAdapter {
        @Override public ComputeBackendType supportedBackendType() { return ComputeBackendType.LOCAL_GPU; }
        @Override public String submitJob(TrainingJob job, ComputeBackend backend, String datasetExportUri) {
            return "ext-job-1";
        }
        @Override public TrainingJobStatusResult checkStatus(String id, ComputeBackend b) {
            return new TrainingJobStatusResult("COMPLETED", null, null);
        }
        @Override public ai.datalithix.kanon.training.model.TrainingMetrics pollMetrics(String id, ComputeBackend b) {
            return new ai.datalithix.kanon.training.model.TrainingMetrics(0.1, 0.95, java.util.Map.of(), 1, 1, Instant.now());
        }
        @Override public boolean cancelJob(String id, ComputeBackend b) { return true; }
        @Override public boolean healthCheck(ComputeBackend b) { return true; }
    }
}

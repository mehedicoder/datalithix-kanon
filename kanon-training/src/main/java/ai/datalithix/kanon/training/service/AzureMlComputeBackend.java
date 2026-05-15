package ai.datalithix.kanon.training.service;

import ai.datalithix.kanon.training.model.ComputeBackend;
import ai.datalithix.kanon.training.model.ComputeBackendType;
import ai.datalithix.kanon.training.model.TrainingJob;
import ai.datalithix.kanon.training.model.TrainingMetrics;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AzureMlComputeBackend implements ComputeBackendAdapter {
    @Override
    public ComputeBackendType supportedBackendType() {
        return ComputeBackendType.AZURE_ML;
    }

    @Override
    public String submitJob(TrainingJob job, ComputeBackend backend, String datasetExportUri) {
        return "azure-ml-" + UUID.randomUUID();
    }

    @Override
    public TrainingJobStatusResult checkStatus(String externalJobId, ComputeBackend backend) {
        return new TrainingJobStatusResult("RUNNING", null, null);
    }

    @Override
    public TrainingMetrics pollMetrics(String externalJobId, ComputeBackend backend) {
        return new TrainingMetrics(0.14, 0.92, java.util.Map.of(), 1, 2, Instant.now());
    }

    @Override
    public boolean cancelJob(String externalJobId, ComputeBackend backend) {
        return true;
    }

    @Override
    public boolean healthCheck(ComputeBackend backend) {
        return backend.configuration() != null && backend.configuration().containsKey("workspaceId");
    }
}

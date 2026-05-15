package ai.datalithix.kanon.training.service;

import ai.datalithix.kanon.training.model.ComputeBackend;
import ai.datalithix.kanon.training.model.ComputeBackendType;
import ai.datalithix.kanon.training.model.HyperParameterConfig;
import ai.datalithix.kanon.training.model.TrainingJob;
import ai.datalithix.kanon.training.model.TrainingMetrics;

public interface ComputeBackendAdapter {
    ComputeBackendType supportedBackendType();
    String submitJob(TrainingJob job, ComputeBackend backend, String datasetExportUri);
    TrainingJobStatusResult checkStatus(String externalJobId, ComputeBackend backend);
    TrainingMetrics pollMetrics(String externalJobId, ComputeBackend backend);
    boolean cancelJob(String externalJobId, ComputeBackend backend);
    boolean healthCheck(ComputeBackend backend);

    record TrainingJobStatusResult(String status, String failureReason, String checkpointUri) {}
}

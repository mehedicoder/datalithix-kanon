package ai.datalithix.kanon.training.service;

import ai.datalithix.kanon.training.model.ComputeBackend;
import ai.datalithix.kanon.training.model.HyperParameterConfig;
import ai.datalithix.kanon.training.model.TrainingJob;
import ai.datalithix.kanon.training.model.TrainingJobStatus;
import java.util.List;

public interface TrainingOrchestrationService {
    TrainingJob submitJob(String tenantId, String datasetVersionId, String datasetDefinitionId,
                          String computeBackendId, String modelName, HyperParameterConfig hyperParameters,
                          String actorId);
    TrainingJob cancelJob(String tenantId, String trainingJobId);
    TrainingJob updateJobStatus(String tenantId, String trainingJobId, TrainingJobStatus newStatus, String actorId);
    TrainingJob getJobStatus(String tenantId, String trainingJobId);
    List<TrainingJob> listJobs(String tenantId);
    List<TrainingJob> listJobsByDataset(String tenantId, String datasetVersionId);
    ComputeBackend registerBackend(ComputeBackend backend);
    boolean healthCheckBackend(String tenantId, String backendId);
}

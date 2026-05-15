package ai.datalithix.kanon.training.service;

import ai.datalithix.kanon.common.service.PagedQueryPort;
import ai.datalithix.kanon.training.model.ComputeBackend;
import ai.datalithix.kanon.training.model.TrainingJob;
import java.util.List;
import java.util.Optional;

public interface TrainingJobRepository extends PagedQueryPort<TrainingJob> {
    TrainingJob save(TrainingJob job);
    Optional<TrainingJob> findById(String tenantId, String trainingJobId);
    List<TrainingJob> findByTenant(String tenantId);
    List<TrainingJob> findByDatasetVersion(String tenantId, String datasetVersionId);
    List<TrainingJob> findByStatus(String tenantId, String status);
    void deleteById(String tenantId, String trainingJobId);

    ComputeBackend saveBackend(ComputeBackend backend);
    Optional<ComputeBackend> findBackendById(String tenantId, String backendId);
    List<ComputeBackend> findBackendsByTenant(String tenantId);
    List<ComputeBackend> findEnabledBackends(String tenantId);
}

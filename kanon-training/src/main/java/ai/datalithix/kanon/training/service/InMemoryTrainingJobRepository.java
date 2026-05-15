package ai.datalithix.kanon.training.service;

import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.training.model.ComputeBackend;
import ai.datalithix.kanon.training.model.TrainingJob;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTrainingJobRepository implements TrainingJobRepository {
    private final Map<String, TrainingJob> jobs = new ConcurrentHashMap<>();
    private final Map<String, ComputeBackend> backends = new ConcurrentHashMap<>();

    @Override
    public TrainingJob save(TrainingJob job) {
        jobs.put(job.trainingJobId(), job);
        return job;
    }

    @Override
    public Optional<TrainingJob> findById(String tenantId, String trainingJobId) {
        return Optional.ofNullable(jobs.get(trainingJobId)).filter(j -> j.tenantId().equals(tenantId));
    }

    @Override
    public List<TrainingJob> findByTenant(String tenantId) {
        return jobs.values().stream().filter(j -> j.tenantId().equals(tenantId)).toList();
    }

    @Override
    public List<TrainingJob> findByDatasetVersion(String tenantId, String datasetVersionId) {
        return jobs.values().stream()
                .filter(j -> j.tenantId().equals(tenantId))
                .filter(j -> j.datasetVersionId().equals(datasetVersionId))
                .toList();
    }

    @Override
    public List<TrainingJob> findByStatus(String tenantId, String status) {
        return jobs.values().stream()
                .filter(j -> j.tenantId().equals(tenantId))
                .filter(j -> j.status().name().equals(status))
                .toList();
    }

    @Override
    public void deleteById(String tenantId, String trainingJobId) {
        findById(tenantId, trainingJobId).ifPresent(j -> jobs.remove(j.trainingJobId()));
    }

    @Override
    public ComputeBackend saveBackend(ComputeBackend backend) {
        backends.put(backend.backendId(), backend);
        return backend;
    }

    @Override
    public Optional<ComputeBackend> findBackendById(String tenantId, String backendId) {
        return Optional.ofNullable(backends.get(backendId)).filter(b -> b.tenantId().equals(tenantId));
    }

    @Override
    public List<ComputeBackend> findBackendsByTenant(String tenantId) {
        return backends.values().stream().filter(b -> b.tenantId().equals(tenantId)).toList();
    }

    @Override
    public List<ComputeBackend> findEnabledBackends(String tenantId) {
        return backends.values().stream()
                .filter(b -> b.tenantId().equals(tenantId))
                .filter(ComputeBackend::enabled)
                .toList();
    }

    @Override
    public PageResult<TrainingJob> findPage(QuerySpec query) {
        List<TrainingJob> items = findByTenant(query.tenantId());
        int offset = query.page().pageNumber() * query.page().pageSize();
        List<TrainingJob> page = items.stream().skip(offset).limit(query.page().pageSize()).toList();
        return new PageResult<>(page, query.page().pageNumber(), query.page().pageSize(), items.size());
    }
}

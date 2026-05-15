package ai.datalithix.kanon.modelregistry.service;

import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.modelregistry.model.DeploymentTarget;
import ai.datalithix.kanon.modelregistry.model.EvaluationRun;
import ai.datalithix.kanon.modelregistry.model.ModelEntry;
import ai.datalithix.kanon.modelregistry.model.ModelLifecycleStage;
import ai.datalithix.kanon.modelregistry.model.ModelVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryModelRegistryRepository implements ModelRegistryRepository {
    private final Map<String, ModelEntry> entries = new ConcurrentHashMap<>();
    private final Map<String, ModelVersion> versions = new ConcurrentHashMap<>();
    private final Map<String, EvaluationRun> evaluations = new ConcurrentHashMap<>();
    private final Map<String, DeploymentTarget> deployments = new ConcurrentHashMap<>();

    @Override
    public ModelEntry saveEntry(ModelEntry entry) {
        entries.put(entry.modelEntryId(), entry);
        return entry;
    }

    @Override
    public Optional<ModelEntry> findEntryById(String tenantId, String modelEntryId) {
        return Optional.ofNullable(entries.get(modelEntryId)).filter(e -> e.tenantId().equals(tenantId));
    }

    @Override
    public List<ModelEntry> findEntriesByTenant(String tenantId) {
        return entries.values().stream().filter(e -> e.tenantId().equals(tenantId)).toList();
    }

    @Override
    public List<ModelEntry> findEntriesByName(String tenantId, String modelName) {
        return entries.values().stream()
                .filter(e -> e.tenantId().equals(tenantId))
                .filter(e -> e.modelName().equals(modelName))
                .toList();
    }

    @Override
    public void deleteEntryById(String tenantId, String modelEntryId) {
        findEntryById(tenantId, modelEntryId).ifPresent(e -> entries.remove(e.modelEntryId()));
    }

    @Override
    public ModelVersion saveVersion(ModelVersion version) {
        versions.put(version.modelVersionId(), version);
        return version;
    }

    @Override
    public Optional<ModelVersion> findVersionById(String tenantId, String modelVersionId) {
        return Optional.ofNullable(versions.get(modelVersionId)).filter(v -> v.tenantId().equals(tenantId));
    }

    @Override
    public List<ModelVersion> findVersionsByEntryId(String tenantId, String modelEntryId) {
        return versions.values().stream()
                .filter(v -> v.tenantId().equals(tenantId))
                .filter(v -> v.modelEntryId().equals(modelEntryId))
                .sorted((a, b) -> Integer.compare(b.versionNumber(), a.versionNumber()))
                .toList();
    }

    @Override
    public Optional<ModelVersion> findLatestVersion(String tenantId, String modelEntryId) {
        return findVersionsByEntryId(tenantId, modelEntryId).stream().findFirst();
    }

    @Override
    public List<ModelVersion> findVersionsByStage(String tenantId, ModelLifecycleStage stage) {
        return versions.values().stream()
                .filter(v -> v.tenantId().equals(tenantId))
                .filter(v -> v.lifecycleStage() == stage)
                .toList();
    }

    @Override
    public EvaluationRun saveEvaluation(EvaluationRun run) {
        evaluations.put(run.evaluationRunId(), run);
        return run;
    }

    @Override
    public Optional<EvaluationRun> findEvaluationById(String tenantId, String evaluationRunId) {
        return Optional.ofNullable(evaluations.get(evaluationRunId)).filter(e -> e.tenantId().equals(tenantId));
    }

    @Override
    public List<EvaluationRun> findEvaluationsByVersion(String tenantId, String modelVersionId) {
        return evaluations.values().stream()
                .filter(e -> e.tenantId().equals(tenantId))
                .filter(e -> e.modelVersionId().equals(modelVersionId))
                .toList();
    }

    @Override
    public DeploymentTarget saveDeployment(DeploymentTarget target) {
        deployments.put(target.deploymentTargetId(), target);
        return target;
    }

    @Override
    public Optional<DeploymentTarget> findDeploymentById(String tenantId, String deploymentTargetId) {
        return Optional.ofNullable(deployments.get(deploymentTargetId)).filter(d -> d.tenantId().equals(tenantId));
    }

    @Override
    public List<DeploymentTarget> findDeploymentsByVersion(String tenantId, String modelVersionId) {
        return deployments.values().stream()
                .filter(d -> d.tenantId().equals(tenantId))
                .filter(d -> d.modelVersionId().equals(modelVersionId))
                .toList();
    }

    @Override
    public List<DeploymentTarget> findActiveDeployments(String tenantId) {
        return deployments.values().stream()
                .filter(d -> d.tenantId().equals(tenantId))
                .filter(DeploymentTarget::active)
                .toList();
    }

    @Override
    public PageResult<ModelEntry> findPage(QuerySpec query) {
        List<ModelEntry> items = findEntriesByTenant(query.tenantId());
        int offset = query.page().pageNumber() * query.page().pageSize();
        List<ModelEntry> page = items.stream().skip(offset).limit(query.page().pageSize()).toList();
        return new PageResult<>(page, query.page().pageNumber(), query.page().pageSize(), items.size());
    }
}

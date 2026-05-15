package ai.datalithix.kanon.modelregistry.service;

import ai.datalithix.kanon.common.service.PagedQueryPort;
import ai.datalithix.kanon.modelregistry.model.DeploymentTarget;
import ai.datalithix.kanon.modelregistry.model.EvaluationRun;
import ai.datalithix.kanon.modelregistry.model.ModelEntry;
import ai.datalithix.kanon.modelregistry.model.ModelVersion;
import ai.datalithix.kanon.modelregistry.model.ModelLifecycleStage;
import java.util.List;
import java.util.Optional;

public interface ModelRegistryRepository extends PagedQueryPort<ModelEntry> {
    ModelEntry saveEntry(ModelEntry entry);
    Optional<ModelEntry> findEntryById(String tenantId, String modelEntryId);
    List<ModelEntry> findEntriesByTenant(String tenantId);
    List<ModelEntry> findEntriesByName(String tenantId, String modelName);
    void deleteEntryById(String tenantId, String modelEntryId);

    ModelVersion saveVersion(ModelVersion version);
    Optional<ModelVersion> findVersionById(String tenantId, String modelVersionId);
    List<ModelVersion> findVersionsByEntryId(String tenantId, String modelEntryId);
    Optional<ModelVersion> findLatestVersion(String tenantId, String modelEntryId);
    List<ModelVersion> findVersionsByStage(String tenantId, ModelLifecycleStage stage);

    EvaluationRun saveEvaluation(EvaluationRun run);
    Optional<EvaluationRun> findEvaluationById(String tenantId, String evaluationRunId);
    List<EvaluationRun> findEvaluationsByVersion(String tenantId, String modelVersionId);

    DeploymentTarget saveDeployment(DeploymentTarget target);
    Optional<DeploymentTarget> findDeploymentById(String tenantId, String deploymentTargetId);
    List<DeploymentTarget> findDeploymentsByVersion(String tenantId, String modelVersionId);
    List<DeploymentTarget> findActiveDeployments(String tenantId);
}

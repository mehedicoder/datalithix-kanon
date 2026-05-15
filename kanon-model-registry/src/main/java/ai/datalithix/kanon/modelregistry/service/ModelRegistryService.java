package ai.datalithix.kanon.modelregistry.service;

import ai.datalithix.kanon.modelregistry.model.ModelEntry;
import ai.datalithix.kanon.modelregistry.model.ModelLifecycleStage;
import ai.datalithix.kanon.modelregistry.model.ModelVersion;
import java.util.List;

public interface ModelRegistryService {
    ModelEntry registerModel(String tenantId, String modelName, String framework, String taskType,
                              String domainType, String artifactUri, String trainingJobId,
                              String datasetVersionId, String actorId);
    ModelVersion promoteModel(String tenantId, String modelVersionId, ModelLifecycleStage targetStage, String actorId);
    ModelVersion rollbackModel(String tenantId, String modelEntryId, int targetVersionNumber, String actorId);
    ModelEntry getModelEntry(String tenantId, String modelEntryId);
    ModelVersion getModelVersion(String tenantId, String modelVersionId);
    List<ModelEntry> listModels(String tenantId);
    List<ModelVersion> listVersions(String tenantId, String modelEntryId);
    List<ModelVersion> listModelsByStage(String tenantId, ModelLifecycleStage stage);
}

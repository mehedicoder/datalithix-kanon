package ai.datalithix.kanon.modelregistry.service;

import ai.datalithix.kanon.modelregistry.model.EvaluationMetric;
import ai.datalithix.kanon.modelregistry.model.EvaluationRun;
import ai.datalithix.kanon.modelregistry.model.ModelVersion;
import java.util.List;

public interface EvaluationService {
    EvaluationRun evaluate(String tenantId, String modelVersionId, String testDatasetVersionId, String actorId);
    EvaluationRun evaluateWithThreshold(String tenantId, String modelVersionId, String testDatasetVersionId,
                                         double minThreshold, String actorId);
    List<EvaluationRun> getEvaluationHistory(String tenantId, String modelVersionId);
    ModelVersion compareVersions(String tenantId, String modelEntryId, int versionA, int versionB);
    List<EvaluationMetric> computeDefaultMetrics(String tenantId, String modelVersionId, String testDatasetVersionId);
}

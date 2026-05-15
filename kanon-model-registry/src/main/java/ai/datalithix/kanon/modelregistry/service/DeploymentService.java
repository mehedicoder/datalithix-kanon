package ai.datalithix.kanon.modelregistry.service;

import ai.datalithix.kanon.modelregistry.model.DeploymentConfig;
import ai.datalithix.kanon.modelregistry.model.DeploymentTarget;
import java.util.List;

public interface DeploymentService {
    DeploymentTarget deploy(String tenantId, String modelVersionId, String targetType,
                             String endpointUrl, DeploymentConfig config, String actorId);
    DeploymentTarget rollback(String tenantId, String deploymentTargetId, String actorId);
    boolean healthCheck(String tenantId, String deploymentTargetId);
    List<DeploymentTarget> getActiveDeployments(String tenantId);
    List<DeploymentTarget> getDeploymentsByVersion(String tenantId, String modelVersionId);
}

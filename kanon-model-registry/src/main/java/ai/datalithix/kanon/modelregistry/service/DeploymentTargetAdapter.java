package ai.datalithix.kanon.modelregistry.service;

import ai.datalithix.kanon.modelregistry.model.DeploymentConfig;
import ai.datalithix.kanon.modelregistry.model.ModelVersion;

public interface DeploymentTargetAdapter {
    String supportedTargetType();
    DeployResult deploy(ModelVersion version, String endpointUrl, DeploymentConfig config);
    boolean healthCheck(String endpointUrl);
    boolean rollback(String endpointUrl, String deploymentId);

    record DeployResult(String deploymentId, String healthStatus, boolean success, String failureReason) {}
}

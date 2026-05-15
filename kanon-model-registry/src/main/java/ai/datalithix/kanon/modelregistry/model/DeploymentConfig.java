package ai.datalithix.kanon.modelregistry.model;

import java.util.Map;

public record DeploymentConfig(
        String deploymentStrategy,
        Map<String, String> environmentVariables,
        String healthCheckEndpoint,
        int replicas,
        boolean autoRollback
) {
    public DeploymentConfig {
        environmentVariables = environmentVariables == null ? Map.of() : Map.copyOf(environmentVariables);
    }
}

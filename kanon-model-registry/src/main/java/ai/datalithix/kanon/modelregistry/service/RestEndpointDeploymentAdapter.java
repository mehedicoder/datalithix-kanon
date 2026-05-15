package ai.datalithix.kanon.modelregistry.service;

import ai.datalithix.kanon.modelregistry.model.DeploymentConfig;
import ai.datalithix.kanon.modelregistry.model.ModelVersion;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RestEndpointDeploymentAdapter implements DeploymentTargetAdapter {
    private static final Logger log = LoggerFactory.getLogger(RestEndpointDeploymentAdapter.class);
    private final HttpClient httpClient;

    public RestEndpointDeploymentAdapter() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String supportedTargetType() {
        return "REST_ENDPOINT";
    }

    @Override
    public DeployResult deploy(ModelVersion version, String endpointUrl, DeploymentConfig config) {
        try {
            var body = new java.util.HashMap<String, Object>();
            body.put("modelVersionId", version.modelVersionId());
            body.put("modelEntryId", version.modelEntryId());
            body.put("artifactUri", version.artifact().artifactUri());
            body.put("strategy", config.deploymentStrategy());
            body.put("replicas", config.replicas());
            body.put("autoRollback", config.autoRollback());
            if (config.environmentVariables() != null) {
                body.put("environment", config.environmentVariables());
            }
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl + "/deploy"))
                    .timeout(Duration.ofMinutes(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                var deploymentId = "dep-" + UUID.randomUUID();
                log.info("Deployed model {} to {}: {}", version.modelVersionId(), endpointUrl, response.statusCode());
                return new DeployResult(deploymentId, "HEALTHY", true, null);
            }
            return new DeployResult(null, "UNHEALTHY", false, "HTTP " + response.statusCode());
        } catch (Exception e) {
            log.warn("Deployment failed for {} to {}: {}", version.modelVersionId(), endpointUrl, e.getMessage());
            return new DeployResult(null, "UNHEALTHY", false, e.getMessage());
        }
    }

    @Override
    public boolean healthCheck(String endpointUrl) {
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl + "/health"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 400;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean rollback(String endpointUrl, String deploymentId) {
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl + "/rollback/" + deploymentId))
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            log.warn("Rollback failed for {}: {}", deploymentId, e.getMessage());
            return false;
        }
    }
}

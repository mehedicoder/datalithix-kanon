package ai.datalithix.kanon.modelregistry.service;

import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.modelregistry.model.DeploymentConfig;
import ai.datalithix.kanon.modelregistry.model.ModelArtifact;
import ai.datalithix.kanon.modelregistry.model.ModelLifecycleStage;
import ai.datalithix.kanon.modelregistry.model.ModelVersion;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RestEndpointDeploymentAdapterTest {

    private final RestEndpointDeploymentAdapter adapter = new RestEndpointDeploymentAdapter();
    private final ModelVersion version = new ModelVersion("mv-1", "me-1", "tenant-1", 1, "tj-1",
            "dsv-1", null,
            new ModelArtifact("s3://bucket/model.pt", "pytorch", 0, null, "S3"),
            null, Set.of(), ModelLifecycleStage.PRODUCTION, null, null,
            List.of(), List.of(), List.of(),
            new AuditMetadata(Instant.now(), "creator", Instant.now(), "creator", 1));

    @Test
    void supportedTargetTypeIsRestEndpoint() {
        assertEquals("REST_ENDPOINT", adapter.supportedTargetType());
    }

    @Test
    void deployReturnsFailedResultForNonexistentEndpoint() {
        var config = new DeploymentConfig("BLUE_GREEN", Map.of(), "/health", 2, true);
        var result = adapter.deploy(version, "http://localhost:1/nonexistent", config);
        assertFalse(result.success());
    }

    @Test
    void healthCheckReturnsFalseForNonexistentEndpoint() {
        assertFalse(adapter.healthCheck("http://localhost:1/nonexistent"));
    }

    @Test
    void rollbackReturnsFalseForNonexistentEndpoint() {
        assertFalse(adapter.rollback("http://localhost:1/nonexistent", "dep-1"));
    }

    @Test
    void deployResultRecord() {
        var result = new DeploymentTargetAdapter.DeployResult("dep-1", "HEALTHY", true, null);
        assertEquals("dep-1", result.deploymentId());
        assertEquals("HEALTHY", result.healthStatus());
        assertTrue(result.success());
        assertNull(result.failureReason());
    }
}

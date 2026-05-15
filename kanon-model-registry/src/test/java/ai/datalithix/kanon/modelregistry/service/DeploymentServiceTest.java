package ai.datalithix.kanon.modelregistry.service;

import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.evidence.model.EvidenceEvent;
import ai.datalithix.kanon.evidence.service.EvidenceLedger;
import ai.datalithix.kanon.modelregistry.model.DeploymentConfig;
import ai.datalithix.kanon.modelregistry.model.DeploymentTarget;
import ai.datalithix.kanon.modelregistry.model.ModelArtifact;
import ai.datalithix.kanon.modelregistry.model.ModelLifecycleStage;
import ai.datalithix.kanon.modelregistry.model.ModelVersion;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeploymentServiceTest {
    private final InMemoryModelRegistryRepository repository = new InMemoryModelRegistryRepository();
    private final CapturingEvidenceLedger ledger = new CapturingEvidenceLedger();
    private final DefaultDeploymentService service = new DefaultDeploymentService(repository, ledger, List.of());

    @Test
    void deploysProductionModel() {
        ModelVersion version = setupVersion(ModelLifecycleStage.PRODUCTION);

        DeploymentTarget target = service.deploy("tenant-1", version.modelVersionId(),
                "REST_API", "http://serve/v1", defaultConfig(), "user-1");

        assertNotNull(target);
        assertTrue(target.active());
        assertEquals("REST_API", target.targetType());
    }

    @Test
    void deploysStagingModel() {
        ModelVersion version = setupVersion(ModelLifecycleStage.STAGING);

        DeploymentTarget target = service.deploy("tenant-1", version.modelVersionId(),
                "REST_API", "http://staging/v1", defaultConfig(), "user-1");

        assertNotNull(target);
        assertTrue(target.active());
    }

    @Test
    void rejectsDeployOfDevelopmentModel() {
        ModelVersion version = setupVersion(ModelLifecycleStage.DEVELOPMENT);

        assertThrows(IllegalStateException.class, () ->
                service.deploy("tenant-1", version.modelVersionId(),
                        "REST_API", "http://dev/v1", defaultConfig(), "user-1"));
    }

    @Test
    void rollsBackActiveDeployment() {
        ModelVersion version = setupVersion(ModelLifecycleStage.PRODUCTION);
        DeploymentTarget target = service.deploy("tenant-1", version.modelVersionId(),
                "REST_API", "http://serve/v1", defaultConfig(), "user-1");

        DeploymentTarget rolledBack = service.rollback("tenant-1", target.deploymentTargetId(), "admin-1");

        assertFalse(rolledBack.active());
        assertNotNull(rolledBack.rolledBackAt());
    }

    @Test
    void rejectsRollbackOfInactiveDeployment() {
        ModelVersion version = setupVersion(ModelLifecycleStage.PRODUCTION);
        DeploymentTarget target = service.deploy("tenant-1", version.modelVersionId(),
                "REST_API", "http://serve/v1", defaultConfig(), "user-1");
        service.rollback("tenant-1", target.deploymentTargetId(), "admin-1");

        assertThrows(IllegalStateException.class, () ->
                service.rollback("tenant-1", target.deploymentTargetId(), "admin-2"));
    }

    @Test
    void healthCheckPassesForValidEndpoint() {
        ModelVersion version = setupVersion(ModelLifecycleStage.PRODUCTION);
        DeploymentTarget target = service.deploy("tenant-1", version.modelVersionId(),
                "REST_API", "http://serve/v1", defaultConfig(), "user-1");

        boolean healthy = service.healthCheck("tenant-1", target.deploymentTargetId());

        assertTrue(healthy);
    }

    @Test
    void listsActiveDeployments() {
        ModelVersion v1 = setupVersion(ModelLifecycleStage.PRODUCTION, "me-1");
        ModelVersion v2 = setupVersion(ModelLifecycleStage.PRODUCTION, "me-2");
        service.deploy("tenant-1", v1.modelVersionId(), "REST_API", "http://v1", defaultConfig(), "user-1");
        service.deploy("tenant-1", v2.modelVersionId(), "REST_API", "http://v2", defaultConfig(), "user-1");

        List<DeploymentTarget> active = service.getActiveDeployments("tenant-1");

        assertEquals(2, active.size());
    }

    @Test
    void enforcesTenantIsolationOnDeployments() {
        ModelVersion version = setupVersion(ModelLifecycleStage.PRODUCTION);
        service.deploy("tenant-1", version.modelVersionId(), "REST_API", "http://v1", defaultConfig(), "user-1");

        List<DeploymentTarget> active = service.getActiveDeployments("other-tenant");

        assertTrue(active.isEmpty());
    }

    private ModelVersion setupVersion(ModelLifecycleStage stage) {
        return setupVersion(stage, "me-1");
    }

    private ModelVersion setupVersion(ModelLifecycleStage stage, String modelEntryId) {
        Instant now = Instant.now();
        AuditMetadata audit = new AuditMetadata(now, "creator", now, "creator", 1);
        ModelArtifact artifact = new ModelArtifact("s3://models/test/v1/model.pt", "pytorch", 0, null, "S3");
        ModelVersion version = new ModelVersion("mv-1", modelEntryId, "tenant-1", 1, "tj-1",
                "dsv-1", null, artifact, null, java.util.Set.of(),
                stage, null, null, List.of(), List.of(), List.of(), audit);
        repository.saveVersion(version);
        return version;
    }

    private static DeploymentConfig defaultConfig() {
        return new DeploymentConfig("BLUE_GREEN", java.util.Map.of(), "/health", 2, true);
    }

    private static class CapturingEvidenceLedger implements EvidenceLedger {
        @Override public void append(EvidenceEvent event) {}
    }
}

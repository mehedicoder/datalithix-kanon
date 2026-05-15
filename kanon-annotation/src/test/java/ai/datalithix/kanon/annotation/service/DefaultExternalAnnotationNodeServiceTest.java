package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.AnnotationNodeVerificationResult;
import ai.datalithix.kanon.annotation.model.AnnotationNodeVerificationStep;
import ai.datalithix.kanon.annotation.model.AnnotationNodeVerificationStepStatus;
import ai.datalithix.kanon.annotation.model.ExternalAnnotationNode;
import ai.datalithix.kanon.annotation.model.ExternalAnnotationNodeStatus;
import ai.datalithix.kanon.annotation.model.ExternalAnnotationProviderType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.evidence.service.InMemoryEvidenceLedger;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultExternalAnnotationNodeServiceTest {
    @Test
    void tenantAdminCannotSeeOtherTenantNodes() {
        InMemoryExternalAnnotationNodeRepository repository = new InMemoryExternalAnnotationNodeRepository();
        repository.save(node("tenant-b", "node-1"));
        DefaultExternalAnnotationNodeService service = service(repository);

        List<ExternalAnnotationNode> visible = service.list("tenant-a", false);

        assertTrue(visible.isEmpty());
    }

    @Test
    void tenantAdminCannotTestConnectionForOtherTenantNode() {
        InMemoryExternalAnnotationNodeRepository repository = new InMemoryExternalAnnotationNodeRepository();
        repository.save(node("tenant-b", "node-1"));
        DefaultExternalAnnotationNodeService service = service(repository);

        assertThrows(IllegalArgumentException.class, () -> service.testConnection("tenant-a", false, "node-1", "tenant-admin"));
    }

    private static DefaultExternalAnnotationNodeService service(InMemoryExternalAnnotationNodeRepository repository) {
        return new DefaultExternalAnnotationNodeService(
                repository,
                node -> new AnnotationNodeVerificationResult(
                        node.nodeId(),
                        ExternalAnnotationNodeStatus.ACTIVE,
                        List.of(new AnnotationNodeVerificationStep("dns-resolve", AnnotationNodeVerificationStepStatus.PASSED, "ok", 1)),
                        "1.0.0",
                        1,
                        Instant.now()
                ),
                new NoopExternalAnnotationNodeUsageGuard(),
                new InMemoryEvidenceLedger()
        );
    }

    private static ExternalAnnotationNode node(String tenantId, String nodeId) {
        Instant now = Instant.now();
        return new ExternalAnnotationNode(
                nodeId,
                tenantId,
                "Node",
                ExternalAnnotationProviderType.CVAT,
                "http://localhost:8080",
                "env:CVAT_TOKEN",
                "bucket",
                ExternalAnnotationNodeStatus.OFFLINE,
                null,
                null,
                null,
                true,
                new AuditMetadata(now, "seed@" + tenantId, now, "seed@" + tenantId, 0)
        );
    }
}

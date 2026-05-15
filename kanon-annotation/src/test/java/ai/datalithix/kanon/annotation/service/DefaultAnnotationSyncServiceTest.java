package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.AnnotationExecutionNodeType;
import ai.datalithix.kanon.annotation.model.AnnotationGeometryType;
import ai.datalithix.kanon.annotation.model.AnnotationNodeDescriptor;
import ai.datalithix.kanon.annotation.model.AnnotationResult;
import ai.datalithix.kanon.annotation.model.AnnotationResultItem;
import ai.datalithix.kanon.annotation.model.AnnotationTask;
import ai.datalithix.kanon.annotation.model.AnnotationTaskCreation;
import ai.datalithix.kanon.annotation.model.AnnotationTaskStatus;
import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.evidence.service.InMemoryEvidenceLedger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultAnnotationSyncServiceTest {
    @Test
    void pushesTaskToSelectedNodeAndAppendsEvidence() {
        InMemoryEvidenceLedger evidenceLedger = new InMemoryEvidenceLedger();
        DefaultAnnotationSyncService service = new DefaultAnnotationSyncService(
                new DefaultAnnotationNodeRegistry(List.of(new FakeAnnotationNode())),
                evidenceLedger
        );

        AnnotationTaskCreation creation = service.pushTask(task());

        assertEquals("task-1", creation.annotationTaskId());
        assertEquals("external-1", creation.externalTaskId());
        assertEquals("EXTERNAL_ANNOTATION_TASK_PUSHED", evidenceLedger.snapshot().getFirst().eventType());
    }

    @Test
    void syncsResultAndAppendsEvidence() {
        InMemoryEvidenceLedger evidenceLedger = new InMemoryEvidenceLedger();
        DefaultAnnotationSyncService service = new DefaultAnnotationSyncService(
                new DefaultAnnotationNodeRegistry(List.of(new FakeAnnotationNode())),
                evidenceLedger
        );

        AnnotationResult result = service.syncResult("node-1", "external-1");

        assertEquals("task-1", result.annotationTaskId());
        assertEquals(AnnotationTaskStatus.COMPLETED, result.status());
        assertEquals("EXTERNAL_ANNOTATION_TASK_SYNCED", evidenceLedger.snapshot().getFirst().eventType());
    }

    private static AnnotationTask task() {
        return new AnnotationTask(
                "task-1",
                "tenant-a",
                "case-1",
                "workflow-1",
                "trace-1",
                null,
                AssetType.DOCUMENT,
                DomainType.ACCOUNTING,
                AnnotationExecutionNodeType.LABEL_STUDIO,
                Set.of("invoice_number"),
                Map.of("documentUri", "s3://tenant-a/invoice.pdf"),
                Map.of(),
                "evidence-1",
                Instant.parse("2026-04-17T00:00:00Z")
        );
    }

    private static final class FakeAnnotationNode implements AnnotationNode {
        @Override
        public AnnotationNodeDescriptor descriptor() {
            return new AnnotationNodeDescriptor(
                    "node-1",
                    AnnotationExecutionNodeType.LABEL_STUDIO,
                    "Fake Label Studio",
                    Set.of(AssetType.DOCUMENT),
                    Set.of(),
                    Set.of(AnnotationGeometryType.SCENE_LABEL),
                    Set.of("document-field"),
                    true
            );
        }

        @Override
        public boolean supports(AnnotationTask task) {
            return task.assetType() == AssetType.DOCUMENT;
        }

        @Override
        public AnnotationTaskCreation createTask(AnnotationTask task) {
            return new AnnotationTaskCreation(
                    task.annotationTaskId(),
                    "node-1",
                    AnnotationExecutionNodeType.LABEL_STUDIO,
                    "external-1",
                    AnnotationTaskStatus.PUSHED,
                    "http://annotation.local/external-1",
                    Map.of("tenantId", task.tenantId(), "caseId", task.caseId(), "kanonTaskId", task.annotationTaskId()),
                    Instant.parse("2026-04-17T00:01:00Z")
            );
        }

        @Override
        public AnnotationResult fetchResult(String externalTaskId) {
            return new AnnotationResult(
                    "task-1",
                    "node-1",
                    AnnotationExecutionNodeType.LABEL_STUDIO,
                    externalTaskId,
                    AnnotationTaskStatus.COMPLETED,
                    List.of(new AnnotationResultItem(
                            "invoice_number",
                            AnnotationGeometryType.SCENE_LABEL,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "INV-100",
                            "0.99",
                            Map.of()
                    )),
                    "object://raw/external-1.json",
                    null,
                    Map.of("tenantId", "tenant-a", "caseId", "case-1", "kanonTaskId", "task-1"),
                    Instant.parse("2026-04-17T00:02:00Z")
            );
        }
    }
}

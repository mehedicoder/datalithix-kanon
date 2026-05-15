package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.AnnotationExecutionNodeType;
import ai.datalithix.kanon.annotation.model.AnnotationGeometryType;
import ai.datalithix.kanon.annotation.model.AnnotationNodeDescriptor;
import ai.datalithix.kanon.annotation.model.AnnotationResult;
import ai.datalithix.kanon.annotation.model.AnnotationResultItem;
import ai.datalithix.kanon.annotation.model.AnnotationSyncRecord;
import ai.datalithix.kanon.annotation.model.AnnotationTask;
import ai.datalithix.kanon.annotation.model.AnnotationTaskCreation;
import ai.datalithix.kanon.annotation.model.AnnotationTaskStatus;
import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.PageSpec;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.common.model.SortDirection;
import ai.datalithix.kanon.evidence.service.InMemoryEvidenceLedger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnotationTaskOrchestrationServiceTest {
    private final InMemoryEvidenceLedger evidenceLedger = new InMemoryEvidenceLedger();
    private final AnnotationSyncRecordRepository syncRepo = new InMemoryAnnotationSyncRecordRepository();

    @Test
    void pushTaskCreatesSyncRecordAndReturnsCreation() {
        var service = service();
        AnnotationTaskCreation creation = service.pushTask(task());

        assertEquals("task-1", creation.annotationTaskId());
        assertEquals("external-1", creation.externalTaskId());

        var records = syncRepo.findByAnnotationTaskId("tenant-a", "task-1");
        assertEquals(1, records.size());
        assertEquals("external-1", records.getFirst().externalTaskId());
        assertEquals("EXTERNAL_ANNOTATION_TASK_PUSHED", evidenceLedger.snapshot().getFirst().eventType());
    }

    @Test
    void syncResultSavesRecordAndAppendsEvidence() {
        var service = service();
        AnnotationSyncRecord record = service.syncResult("tenant-a", "node-1", "external-1");

        assertEquals("task-1", record.annotationTaskId());
        assertEquals(AnnotationTaskStatus.COMPLETED, record.status());

        var byTask = syncRepo.findByAnnotationTaskId("tenant-a", "task-1");
        assertEquals(1, byTask.size());
        var events = evidenceLedger.snapshot();
        assertTrue(events.stream().anyMatch(e -> "EXTERNAL_ANNOTATION_SYNC_COMPLETED".equals(e.eventType())));
    }

    @Test
    void retryCreatesNewSyncRecord() {
        var service = service();
        service.pushTask(task());
        var records = syncRepo.findByAnnotationTaskId("tenant-a", "task-1");
        assertEquals(1, records.size());

        AnnotationSyncRecord retried = service.retry("tenant-a", "task-1");

        assertNotNull(retried);
        assertEquals("task-1", retried.annotationTaskId());

        var afterRetry = syncRepo.findByAnnotationTaskId("tenant-a", "task-1");
        assertEquals(2, afterRetry.size());
        assertTrue(evidenceLedger.snapshot().stream().anyMatch(e -> "EXTERNAL_ANNOTATION_TASK_RETRIED".equals(e.eventType())));
    }

    @Test
    void retryThrowsOnUnknownTaskId() {
        var service = service();
        assertThrows(IllegalArgumentException.class, () -> service.retry("tenant-a", "unknown-task"));
    }

    @Test
    void findSyncRecordsReturnsPage() {
        var service = service();
        service.syncResult("tenant-a", "node-1", "external-1");

        PageResult<AnnotationSyncRecord> page = service.findSyncRecords(
                new QuerySpec("tenant-a", new PageSpec(0, 10, null, SortDirection.ASC), null, null));

        assertEquals(1, page.totalItems());
    }

    @Test
    void findByTaskIdReturnsRecords() {
        var service = service();
        service.syncResult("tenant-a", "node-1", "external-1");

        var records = service.findByTaskId("tenant-a", "task-1");
        assertEquals(1, records.size());
    }

    private AnnotationTaskOrchestrationService service() {
        return new AnnotationTaskOrchestrationService(
                new DefaultAnnotationSyncService(
                        new DefaultAnnotationNodeRegistry(List.of(new FakeAnnotationNode())),
                        evidenceLedger),
                syncRepo,
                evidenceLedger);
    }

    private static AnnotationTask task() {
        return new AnnotationTask(
                "task-1", "tenant-a", "case-1", "workflow-1", "trace-1",
                null, AssetType.DOCUMENT, DomainType.ACCOUNTING, AnnotationExecutionNodeType.LABEL_STUDIO,
                Set.of("invoice_number"),
                Map.of("documentUri", "s3://tenant-a/invoice.pdf"),
                Map.of(), "evidence-1", Instant.parse("2026-04-17T00:00:00Z"));
    }

    private static final class FakeAnnotationNode implements AnnotationNode {
        @Override
        public AnnotationNodeDescriptor descriptor() {
            return new AnnotationNodeDescriptor(
                    "node-1", AnnotationExecutionNodeType.LABEL_STUDIO, "Fake Label Studio",
                    Set.of(AssetType.DOCUMENT), Set.of(), Set.of(AnnotationGeometryType.SCENE_LABEL),
                    Set.of("document-field"), true);
        }

        @Override
        public boolean supports(AnnotationTask task) {
            return task.assetType() == AssetType.DOCUMENT;
        }

        @Override
        public AnnotationTaskCreation createTask(AnnotationTask task) {
            return new AnnotationTaskCreation(
                    task.annotationTaskId(), "node-1", AnnotationExecutionNodeType.LABEL_STUDIO,
                    "external-1", AnnotationTaskStatus.PUSHED,
                    "http://annotation.local/external-1",
                    Map.of("tenantId", task.tenantId(), "caseId", task.caseId(), "kanonTaskId", task.annotationTaskId()),
                    Instant.parse("2026-04-17T00:01:00Z"));
        }

        @Override
        public AnnotationResult fetchResult(String externalTaskId) {
            return new AnnotationResult(
                    "task-1", "node-1", AnnotationExecutionNodeType.LABEL_STUDIO,
                    externalTaskId, AnnotationTaskStatus.COMPLETED,
                    List.of(new AnnotationResultItem(
                            "invoice_number", AnnotationGeometryType.SCENE_LABEL,
                            null, null, null, null, null, "INV-100", "0.99", Map.of())),
                    "object://raw/external-1.json", null,
                    Map.of("tenantId", "tenant-a", "caseId", "case-1", "kanonTaskId", "task-1"),
                    Instant.parse("2026-04-17T00:02:00Z"));
        }
    }
}

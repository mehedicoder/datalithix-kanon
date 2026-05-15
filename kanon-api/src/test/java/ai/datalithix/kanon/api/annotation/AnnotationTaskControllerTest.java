package ai.datalithix.kanon.api.annotation;

import ai.datalithix.kanon.annotation.model.AnnotationExecutionNodeType;
import ai.datalithix.kanon.annotation.model.AnnotationSyncRecord;
import ai.datalithix.kanon.annotation.model.AnnotationTaskCreation;
import ai.datalithix.kanon.annotation.model.AnnotationTaskStatus;
import ai.datalithix.kanon.annotation.service.AnnotationTaskOrchestrationService;
import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.PageSpec;
import ai.datalithix.kanon.common.model.QuerySpec;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.NOT_FOUND;

class AnnotationTaskControllerTest {
    private final FakeOrchestrationService fakeService = new FakeOrchestrationService();
    private final AnnotationTaskController controller = new AnnotationTaskController(fakeService);

    @Test
    void createTaskDelegatesToService() {
        var request = new AnnotationTaskController.CreateTaskRequest(
                "task-1", "tenant-a", "case-1", "wf-1", "trace-1",
                "media-1", AssetType.VIDEO, DomainType.AGRICULTURE,
                AnnotationExecutionNodeType.CVAT,
                Set.of("tractor"), Map.of("uri", "s3://bucket/video.mp4"),
                Map.of("mode", "auto"), "evt-1");

        AnnotationTaskCreation result = controller.createTask(request);

        assertEquals("task-1", result.annotationTaskId());
        assertEquals("cvat-42", result.externalTaskId());
    }

    @Test
    void syncTaskReturnsSyncRecord() {
        AnnotationSyncRecord record = controller.syncTask("tenant-a", "node-1", "ext-1");
        assertEquals("task-1", record.annotationTaskId());
        assertEquals(AnnotationTaskStatus.COMPLETED, record.status());
    }

    @Test
    void syncTaskThrows404OnUnknown() {
        fakeService.throwOnSync = true;
        var ex = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> controller.syncTask("tenant-a", "unknown-node", "ext-1"));
        assertEquals(NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void retrySyncReturnsSyncRecord() {
        AnnotationSyncRecord record = controller.retrySync("tenant-a", "task-1");
        assertNotNull(record);
        assertEquals("task-1", record.annotationTaskId());
    }

    @Test
    void retrySyncThrows404OnUnknown() {
        fakeService.throwOnRetry = true;
        var ex = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> controller.retrySync("tenant-a", "unknown-task"));
        assertEquals(NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void listSyncRecordsReturnsPage() {
        PageResult<AnnotationSyncRecord> page = controller.listSyncRecords("tenant-a", 0, 10);
        assertEquals(1, page.totalItems());
    }

    @Test
    void getTaskSyncRecordsReturnsList() {
        var records = controller.getTaskSyncRecords("tenant-a", "task-1");
        assertEquals(1, records.size());
    }

    private static final class FakeOrchestrationService extends AnnotationTaskOrchestrationService {
        boolean throwOnSync;
        boolean throwOnRetry;

        FakeOrchestrationService() {
            super(null, null, null);
        }

        @Override
        public AnnotationTaskCreation pushTask(ai.datalithix.kanon.annotation.model.AnnotationTask task) {
            return new AnnotationTaskCreation(
                    task.annotationTaskId(), "cvat-node", AnnotationExecutionNodeType.CVAT,
                    "cvat-42", AnnotationTaskStatus.PUSHED,
                    "http://cvat/tasks/42", Map.of(), Instant.now());
        }

        @Override
        public AnnotationSyncRecord syncResult(String tenantId, String nodeId, String externalTaskId) {
            if (throwOnSync) throw new IllegalArgumentException("Node not found: " + nodeId);
            return new AnnotationSyncRecord(
                    "task-1", nodeId, AnnotationExecutionNodeType.CVAT,
                    externalTaskId, AnnotationTaskStatus.COMPLETED,
                    "http://cvat/tasks/" + externalTaskId, null,
                    Map.of(), Instant.now());
        }

        @Override
        public AnnotationSyncRecord retry(String tenantId, String annotationTaskId) {
            if (throwOnRetry) throw new IllegalArgumentException("No sync records found: " + annotationTaskId);
            return new AnnotationSyncRecord(
                    annotationTaskId, "cvat-node", AnnotationExecutionNodeType.CVAT,
                    "cvat-43", AnnotationTaskStatus.PUSHED,
                    "http://cvat/tasks/43", null,
                    Map.of(), Instant.now());
        }

        @Override
        public PageResult<AnnotationSyncRecord> findSyncRecords(QuerySpec query) {
            return new PageResult<>(List.of(), 0, 10, 1);
        }

        @Override
        public List<AnnotationSyncRecord> findByTaskId(String tenantId, String annotationTaskId) {
            return List.of(new AnnotationSyncRecord(
                    annotationTaskId, "cvat-node", AnnotationExecutionNodeType.CVAT,
                    "ext-1", AnnotationTaskStatus.COMPLETED,
                    null, null, Map.of(), Instant.now()));
        }
    }
}

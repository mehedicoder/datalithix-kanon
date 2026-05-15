package ai.datalithix.kanon.annotation.service.cvat;

import ai.datalithix.kanon.annotation.model.AnnotationExecutionNodeType;
import ai.datalithix.kanon.annotation.model.AnnotationGeometryType;
import ai.datalithix.kanon.annotation.model.AnnotationResultItem;
import ai.datalithix.kanon.annotation.model.AnnotationTask;
import ai.datalithix.kanon.annotation.model.AnnotationTaskStatus;
import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DomainType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CvatAnnotationNodeTest {
    @Test
    void createsVideoTaskWithKanonTaskIdAsCanonicalIdentity() {
        FakeCvatClient client = new FakeCvatClient();
        CvatAnnotationNode node = new CvatAnnotationNode("cvat-main", "CVAT", "project-vision", client);

        var creation = node.createTask(task(AssetType.VIDEO, AnnotationExecutionNodeType.CVAT));

        assertEquals("task-1", client.lastRequest.kanonTaskId());
        assertEquals("media-1", client.lastRequest.mediaAssetId());
        assertEquals("project-vision", client.lastRequest.projectRef());
        assertEquals("cvat-200", creation.externalTaskId());
        assertEquals("task-1", creation.annotationTaskId());
        assertEquals(AnnotationExecutionNodeType.CVAT, creation.nodeType());
    }

    @Test
    void fetchesAndMapsVisionResultBackToKanonTaskId() {
        CvatAnnotationNode node = new CvatAnnotationNode("cvat-main", "CVAT", "project-vision", new FakeCvatClient());

        var result = node.fetchResult("cvat-200");

        assertEquals("task-1", result.annotationTaskId());
        assertEquals("cvat-200", result.externalTaskId());
        assertEquals(AnnotationTaskStatus.COMPLETED, result.status());
        assertEquals(AnnotationGeometryType.BOUNDING_BOX, result.items().getFirst().geometryType());
    }

    @Test
    void rejectsDocumentTask() {
        CvatAnnotationNode node = new CvatAnnotationNode("cvat-main", "CVAT", "project-vision", new FakeCvatClient());

        assertFalse(node.supports(task(AssetType.DOCUMENT, AnnotationExecutionNodeType.CVAT)));
        assertTrue(node.supports(task(AssetType.IMAGE, AnnotationExecutionNodeType.CVAT)));
    }

    private static AnnotationTask task(AssetType assetType, AnnotationExecutionNodeType preferredNodeType) {
        return new AnnotationTask(
                "task-1",
                "tenant-a",
                "case-1",
                "workflow-1",
                "trace-1",
                "media-1",
                assetType,
                DomainType.AGRICULTURE,
                preferredNodeType,
                Set.of("tractor"),
                Map.of("mediaUri", "s3://tenant-a/drone.mp4"),
                Map.of("mode", "mandatory-human"),
                "evidence-1",
                Instant.parse("2026-04-17T00:00:00Z")
        );
    }

    private static final class FakeCvatClient implements CvatClient {
        private CvatTaskRequest lastRequest;

        @Override
        public CvatTaskRef createTask(CvatTaskRequest request) {
            this.lastRequest = request;
            return new CvatTaskRef("cvat-200", "http://cvat.local/tasks/200", Map.of("kanonTaskId", request.kanonTaskId()));
        }

        @Override
        public CvatTaskResult fetchResult(String externalTaskId) {
            return new CvatTaskResult(
                    externalTaskId,
                    AnnotationTaskStatus.COMPLETED,
                    List.of(new AnnotationResultItem(
                            "tractor",
                            AnnotationGeometryType.BOUNDING_BOX,
                            "{\"x\":10,\"y\":20,\"width\":30,\"height\":40}",
                            1,
                            30,
                            0L,
                            1_000L,
                            null,
                            "0.95",
                            Map.of("trackId", "track-1")
                    )),
                    "object://raw-results/cvat-200.json",
                    null,
                    Map.of("kanonTaskId", "task-1"),
                    Instant.parse("2026-04-17T00:01:00Z")
            );
        }
    }
}

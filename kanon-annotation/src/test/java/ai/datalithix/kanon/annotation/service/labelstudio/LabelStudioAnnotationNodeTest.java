package ai.datalithix.kanon.annotation.service.labelstudio;

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

class LabelStudioAnnotationNodeTest {
    @Test
    void createsDocumentTaskWithKanonTaskIdAsCanonicalIdentity() {
        FakeLabelStudioClient client = new FakeLabelStudioClient();
        LabelStudioAnnotationNode node = new LabelStudioAnnotationNode("label-studio-main", "Label Studio", "project-docs", client);

        var creation = node.createTask(task(AssetType.DOCUMENT, AnnotationExecutionNodeType.LABEL_STUDIO));

        assertEquals("task-1", client.lastRequest.kanonTaskId());
        assertEquals("project-docs", client.lastRequest.projectRef());
        assertEquals("ls-100", creation.externalTaskId());
        assertEquals("task-1", creation.annotationTaskId());
        assertEquals(AnnotationExecutionNodeType.LABEL_STUDIO, creation.nodeType());
    }

    @Test
    void fetchesAndMapsResultBackToKanonTaskId() {
        LabelStudioAnnotationNode node = new LabelStudioAnnotationNode("label-studio-main", "Label Studio", "project-docs", new FakeLabelStudioClient());

        var result = node.fetchResult("ls-100");

        assertEquals("task-1", result.annotationTaskId());
        assertEquals("ls-100", result.externalTaskId());
        assertEquals(AnnotationTaskStatus.COMPLETED, result.status());
        assertEquals("invoice_number", result.items().getFirst().label());
    }

    @Test
    void rejectsVisionTask() {
        LabelStudioAnnotationNode node = new LabelStudioAnnotationNode("label-studio-main", "Label Studio", "project-docs", new FakeLabelStudioClient());

        assertFalse(node.supports(task(AssetType.IMAGE, AnnotationExecutionNodeType.LABEL_STUDIO)));
        assertTrue(node.supports(task(AssetType.FORM, AnnotationExecutionNodeType.LABEL_STUDIO)));
    }

    private static AnnotationTask task(AssetType assetType, AnnotationExecutionNodeType preferredNodeType) {
        return new AnnotationTask(
                "task-1",
                "tenant-a",
                "case-1",
                "workflow-1",
                "trace-1",
                null,
                assetType,
                DomainType.ACCOUNTING,
                preferredNodeType,
                Set.of("invoice_number"),
                Map.of("documentUri", "s3://tenant-a/invoice.pdf"),
                Map.of("mode", "human-review"),
                "evidence-1",
                Instant.parse("2026-04-17T00:00:00Z")
        );
    }

    private static final class FakeLabelStudioClient implements LabelStudioClient {
        private LabelStudioTaskRequest lastRequest;

        @Override
        public LabelStudioTaskRef createTask(LabelStudioTaskRequest request) {
            this.lastRequest = request;
            return new LabelStudioTaskRef("ls-100", "http://label-studio.local/tasks/100", Map.of("kanonTaskId", request.kanonTaskId()));
        }

        @Override
        public LabelStudioTaskResult fetchResult(String externalTaskId) {
            return new LabelStudioTaskResult(
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
                            Map.of("field", "invoice_number")
                    )),
                    "object://raw-results/ls-100.json",
                    null,
                    Map.of("kanonTaskId", "task-1"),
                    Instant.parse("2026-04-17T00:01:00Z")
            );
        }
    }
}

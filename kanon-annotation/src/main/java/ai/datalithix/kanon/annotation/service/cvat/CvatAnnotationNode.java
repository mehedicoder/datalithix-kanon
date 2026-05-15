package ai.datalithix.kanon.annotation.service.cvat;

import ai.datalithix.kanon.annotation.model.AnnotationExecutionNodeType;
import ai.datalithix.kanon.annotation.model.AnnotationGeometryType;
import ai.datalithix.kanon.annotation.model.AnnotationNodeDescriptor;
import ai.datalithix.kanon.annotation.model.AnnotationResult;
import ai.datalithix.kanon.annotation.model.AnnotationTask;
import ai.datalithix.kanon.annotation.model.AnnotationTaskCreation;
import ai.datalithix.kanon.annotation.model.AnnotationTaskStatus;
import ai.datalithix.kanon.annotation.service.AnnotationNode;
import ai.datalithix.kanon.common.AssetType;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

public class CvatAnnotationNode implements AnnotationNode {
    private static final Set<AssetType> SUPPORTED_ASSET_TYPES = Set.of(
            AssetType.IMAGE,
            AssetType.VIDEO,
            AssetType.SENSOR_READING,
            AssetType.TELEMETRY
    );

    private final AnnotationNodeDescriptor descriptor;
    private final CvatClient client;
    private final String projectRef;

    public CvatAnnotationNode(String nodeId, String displayName, String projectRef, CvatClient client) {
        if (client == null) {
            throw new IllegalArgumentException("client is required");
        }
        this.client = client;
        this.projectRef = projectRef;
        this.descriptor = new AnnotationNodeDescriptor(
                nodeId,
                AnnotationExecutionNodeType.CVAT,
                displayName,
                SUPPORTED_ASSET_TYPES,
                Set.of(),
                Set.of(
                        AnnotationGeometryType.BOUNDING_BOX,
                        AnnotationGeometryType.POLYGON,
                        AnnotationGeometryType.MASK,
                        AnnotationGeometryType.KEYPOINT,
                        AnnotationGeometryType.TRACK,
                        AnnotationGeometryType.SCENE_LABEL
                ),
                Set.of("object-detection", "segmentation", "tracking", "scene-label", "keypoint"),
                true
        );
    }

    @Override
    public AnnotationNodeDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public boolean supports(AnnotationTask task) {
        return task != null
                && descriptor.enabled()
                && (task.preferredNodeType() == null || task.preferredNodeType() == AnnotationExecutionNodeType.CVAT)
                && SUPPORTED_ASSET_TYPES.contains(task.assetType())
                && (descriptor.supportedDomains().isEmpty() || descriptor.supportedDomains().contains(task.domainType()));
    }

    @Override
    public AnnotationTaskCreation createTask(AnnotationTask task) {
        if (!supports(task)) {
            throw new IllegalArgumentException("CVAT does not support annotation task: " + (task == null ? null : task.annotationTaskId()));
        }
        CvatTaskRef taskRef = client.createTask(new CvatTaskRequest(
                task.annotationTaskId(),
                task.tenantId(),
                task.caseId(),
                projectRef,
                task.assetType(),
                task.domainType(),
                task.mediaAssetId(),
                task.labels(),
                task.payloadRefs(),
                preAnnotations(task),
                task.instructions()
        ));
        return new AnnotationTaskCreation(
                task.annotationTaskId(),
                descriptor.nodeId(),
                descriptor.nodeType(),
                taskRef.externalTaskId(),
                AnnotationTaskStatus.PUSHED,
                taskRef.externalUrl(),
                taskRef.metadata(),
                Instant.now()
        );
    }

    @Override
    public AnnotationResult fetchResult(String externalTaskId) {
        CvatTaskResult result = client.fetchResult(externalTaskId);
        return new AnnotationResult(
                result.metadata().getOrDefault("kanonTaskId", externalTaskId),
                descriptor.nodeId(),
                descriptor.nodeType(),
                result.externalTaskId(),
                result.status(),
                result.items(),
                result.rawResultRef(),
                result.failureReason(),
                result.metadata(),
                result.completedAt()
        );
    }

    private static Map<String, String> preAnnotations(AnnotationTask task) {
        return Map.of(
                "workflowInstanceId", task.workflowInstanceId() == null ? "" : task.workflowInstanceId(),
                "sourceTraceId", task.sourceTraceId() == null ? "" : task.sourceTraceId(),
                "evidenceEventId", task.evidenceEventId() == null ? "" : task.evidenceEventId()
        );
    }
}

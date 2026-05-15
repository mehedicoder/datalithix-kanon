package ai.datalithix.kanon.annotation.service.labelstudio;

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

public class LabelStudioAnnotationNode implements AnnotationNode {
    private static final Set<AssetType> SUPPORTED_ASSET_TYPES = Set.of(
            AssetType.DOCUMENT,
            AssetType.AUDIO,
            AssetType.DATASET,
            AssetType.FORM,
            AssetType.EMAIL
    );

    private final AnnotationNodeDescriptor descriptor;
    private final LabelStudioClient client;
    private final String projectRef;

    public LabelStudioAnnotationNode(String nodeId, String displayName, String projectRef, LabelStudioClient client) {
        if (client == null) {
            throw new IllegalArgumentException("client is required");
        }
        this.client = client;
        this.projectRef = projectRef;
        this.descriptor = new AnnotationNodeDescriptor(
                nodeId,
                AnnotationExecutionNodeType.LABEL_STUDIO,
                displayName,
                SUPPORTED_ASSET_TYPES,
                Set.of(),
                Set.of(AnnotationGeometryType.SCENE_LABEL),
                Set.of("text-span", "document-field", "classification", "tabular-cell", "audio-segment"),
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
                && (task.preferredNodeType() == null || task.preferredNodeType() == AnnotationExecutionNodeType.LABEL_STUDIO)
                && SUPPORTED_ASSET_TYPES.contains(task.assetType())
                && (descriptor.supportedDomains().isEmpty() || descriptor.supportedDomains().contains(task.domainType()));
    }

    @Override
    public AnnotationTaskCreation createTask(AnnotationTask task) {
        if (!supports(task)) {
            throw new IllegalArgumentException("Label Studio does not support annotation task: " + (task == null ? null : task.annotationTaskId()));
        }
        LabelStudioTaskRef taskRef = client.createTask(new LabelStudioTaskRequest(
                task.annotationTaskId(),
                task.tenantId(),
                task.caseId(),
                projectRef,
                task.assetType(),
                task.domainType(),
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
        LabelStudioTaskResult result = client.fetchResult(externalTaskId);
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

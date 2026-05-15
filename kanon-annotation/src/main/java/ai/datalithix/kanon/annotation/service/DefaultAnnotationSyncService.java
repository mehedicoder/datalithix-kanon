package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.AnnotationResult;
import ai.datalithix.kanon.annotation.model.AnnotationSyncRecord;
import ai.datalithix.kanon.annotation.model.AnnotationTask;
import ai.datalithix.kanon.annotation.model.AnnotationTaskCreation;
import ai.datalithix.kanon.annotation.model.AnnotationTaskStatus;
import ai.datalithix.kanon.common.ActorType;
import ai.datalithix.kanon.evidence.model.EvidenceEvent;
import ai.datalithix.kanon.evidence.service.EvidenceLedger;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DefaultAnnotationSyncService implements AnnotationSyncService {
    private final AnnotationNodeRegistry nodeRegistry;
    private final EvidenceLedger evidenceLedger;

    public DefaultAnnotationSyncService(AnnotationNodeRegistry nodeRegistry, EvidenceLedger evidenceLedger) {
        if (nodeRegistry == null) {
            throw new IllegalArgumentException("nodeRegistry is required");
        }
        if (evidenceLedger == null) {
            throw new IllegalArgumentException("evidenceLedger is required");
        }
        this.nodeRegistry = nodeRegistry;
        this.evidenceLedger = evidenceLedger;
    }

    @Override
    public AnnotationTaskCreation pushTask(AnnotationTask task) {
        AnnotationNode node = nodeRegistry.selectNode(task)
                .orElseThrow(() -> new IllegalStateException("No annotation node supports task: " + task.annotationTaskId()));
        try {
            AnnotationTaskCreation creation = node.createTask(task);
            appendEvidence(
                    "EXTERNAL_ANNOTATION_TASK_PUSHED",
                    task.tenantId(),
                    task.caseId(),
                    Map.of("annotationTaskId", task.annotationTaskId()),
                    Map.of(
                            "nodeId", creation.nodeId(),
                            "nodeType", creation.nodeType().name(),
                            "externalTaskId", creation.externalTaskId(),
                            "status", creation.status().name()
                    ),
                    "Annotation task pushed to external annotation node"
            );
            return creation;
        } catch (RuntimeException exception) {
            appendEvidence(
                    "EXTERNAL_ANNOTATION_TASK_FAILED",
                    task.tenantId(),
                    task.caseId(),
                    Map.of("annotationTaskId", task.annotationTaskId()),
                    Map.of("failureReason", exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage()),
                    "Annotation task push failed"
            );
            throw exception;
        }
    }

    @Override
    public AnnotationResult fetchResult(String nodeId, String externalTaskId) {
        AnnotationNode node = node(nodeId);
        return node.fetchResult(externalTaskId);
    }

    @Override
    public AnnotationResult syncResult(String nodeId, String externalTaskId) {
        AnnotationNode node = node(nodeId);
        AnnotationResult result = node.fetchResult(externalTaskId);
        appendEvidence(
                result.status() == AnnotationTaskStatus.FAILED
                        ? "EXTERNAL_ANNOTATION_TASK_SYNC_FAILED"
                        : "EXTERNAL_ANNOTATION_TASK_SYNCED",
                result.metadata().getOrDefault("tenantId", ""),
                result.metadata().get("caseId"),
                Map.of(
                        "nodeId", nodeId,
                        "externalTaskId", externalTaskId
                ),
                Map.of(
                        "annotationTaskId", result.annotationTaskId(),
                        "status", result.status().name(),
                        "itemCount", result.items().size(),
                        "failureReason", result.failureReason() == null ? "" : result.failureReason()
                ),
                "External annotation result synced to Kanon"
        );
        return result;
    }

    public AnnotationSyncRecord syncRecord(String nodeId, String externalTaskId) {
        AnnotationResult result = syncResult(nodeId, externalTaskId);
        return new AnnotationSyncRecord(
                result.annotationTaskId(),
                result.nodeId(),
                result.nodeType(),
                result.externalTaskId(),
                result.status(),
                null,
                result.failureReason(),
                result.metadata(),
                result.completedAt() == null ? Instant.now() : result.completedAt()
        );
    }

    private AnnotationNode node(String nodeId) {
        Optional<AnnotationNode> node = nodeRegistry.findById(nodeId);
        return node.orElseThrow(() -> new IllegalArgumentException("Annotation node not found: " + nodeId));
    }

    private void appendEvidence(
            String eventType,
            String tenantId,
            String caseId,
            Map<String, Object> beforeState,
            Map<String, Object> afterState,
            String rationale
    ) {
        evidenceLedger.append(new EvidenceEvent(
                UUID.randomUUID().toString(),
                tenantId == null ? "" : tenantId,
                caseId,
                eventType,
                ActorType.SYSTEM,
                "kanon-annotation-sync",
                "annotation-sync-service",
                null,
                null,
                beforeState,
                afterState,
                rationale,
                Instant.now()
        ));
    }
}

package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.AnnotationSyncRecord;
import ai.datalithix.kanon.annotation.model.AnnotationTask;
import ai.datalithix.kanon.annotation.model.AnnotationTaskCreation;
import ai.datalithix.kanon.annotation.model.AnnotationTaskStatus;
import ai.datalithix.kanon.common.ActorType;
import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.annotation.model.AnnotationExecutionNodeType;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.evidence.model.EvidenceEvent;
import ai.datalithix.kanon.evidence.service.EvidenceLedger;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AnnotationTaskOrchestrationService {
    private final AnnotationSyncService syncService;
    private final AnnotationSyncRecordRepository syncRecordRepository;
    private final EvidenceLedger evidenceLedger;

    public AnnotationTaskOrchestrationService(
            AnnotationSyncService syncService,
            AnnotationSyncRecordRepository syncRecordRepository,
            EvidenceLedger evidenceLedger
    ) {
        this.syncService = syncService;
        this.syncRecordRepository = syncRecordRepository;
        this.evidenceLedger = evidenceLedger;
    }

    public AnnotationTaskCreation pushTask(AnnotationTask task) {
        AnnotationTaskCreation creation = syncService.pushTask(task);
        var metadata = new HashMap<>(creation.metadata());
        metadata.put("assetType", task.assetType().name());
        metadata.put("domainType", task.domainType().name());
        if (task.caseId() != null) metadata.put("caseId", task.caseId());
        if (task.workflowInstanceId() != null) metadata.put("workflowInstanceId", task.workflowInstanceId());
        if (task.sourceTraceId() != null) metadata.put("sourceTraceId", task.sourceTraceId());
        if (task.mediaAssetId() != null) metadata.put("mediaAssetId", task.mediaAssetId());
        if (task.preferredNodeType() != null) metadata.put("preferredNodeType", task.preferredNodeType().name());
        if (task.evidenceEventId() != null) metadata.put("evidenceEventId", task.evidenceEventId());
        metadata.put("labels", String.join(",", task.labels()));
        var record = new AnnotationSyncRecord(
                task.annotationTaskId(),
                creation.nodeId(),
                creation.nodeType(),
                creation.externalTaskId(),
                creation.status(),
                creation.externalUrl(),
                null,
                Map.copyOf(metadata),
                Instant.now()
        );
        syncRecordRepository.save(task.tenantId(), record);
        return creation;
    }

    public AnnotationSyncRecord syncResult(String tenantId, String nodeId, String externalTaskId) {
        var record = syncService.syncRecord(nodeId, externalTaskId);
        String syncId = syncRecordRepository.save(tenantId, record);
        appendEvidence(
                record.status() == AnnotationTaskStatus.FAILED
                        ? "EXTERNAL_ANNOTATION_SYNC_FAILED"
                        : "EXTERNAL_ANNOTATION_SYNC_COMPLETED",
                tenantId,
                record.annotationTaskId(),
                Map.of("nodeId", nodeId, "externalTaskId", externalTaskId),
                Map.of("status", record.status().name(),
                        "failureReason", record.failureReason() == null ? "" : record.failureReason())
        );
        return record;
    }

    public AnnotationSyncRecord retry(String tenantId, String annotationTaskId) {
        var existing = syncRecordRepository.findByAnnotationTaskId(tenantId, annotationTaskId);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("No sync records found for annotation task: " + annotationTaskId);
        }
        var latest = existing.getFirst();
        var meta = latest.metadata();
        var labels = meta.containsKey("labels") && !meta.get("labels").isBlank()
                ? Set.of(meta.get("labels").split(","))
                : Set.<String>of();
        var task = new AnnotationTask(
                latest.annotationTaskId(),
                tenantId,
                meta.get("caseId"),
                meta.get("workflowInstanceId"),
                meta.get("sourceTraceId"),
                meta.get("mediaAssetId"),
                AssetType.valueOf(meta.get("assetType")),
                DomainType.valueOf(meta.get("domainType")),
                meta.containsKey("preferredNodeType") ? AnnotationExecutionNodeType.valueOf(meta.get("preferredNodeType")) : null,
                labels,
                Map.of(), Map.of(),
                meta.get("evidenceEventId"),
                Instant.now()
        );
        AnnotationTaskCreation creation = syncService.pushTask(task);
        var retryRecord = new AnnotationSyncRecord(
                latest.annotationTaskId(),
                creation.nodeId(),
                creation.nodeType(),
                creation.externalTaskId(),
                creation.status(),
                creation.externalUrl(),
                null,
                creation.metadata(),
                Instant.now()
        );
        syncRecordRepository.save(tenantId, retryRecord);
        appendEvidence("EXTERNAL_ANNOTATION_TASK_RETRIED", tenantId,
                latest.annotationTaskId(),
                Map.of("previousExternalTaskId", latest.externalTaskId()),
                Map.of("newExternalTaskId", creation.externalTaskId()));
        return retryRecord;
    }

    public PageResult<AnnotationSyncRecord> findSyncRecords(QuerySpec query) {
        return syncRecordRepository.findPage(query);
    }

    public List<AnnotationSyncRecord> findByTaskId(String tenantId, String annotationTaskId) {
        return syncRecordRepository.findByAnnotationTaskId(tenantId, annotationTaskId);
    }

    private void appendEvidence(String eventType, String tenantId, String caseId,
                                 Map<String, Object> beforeState, Map<String, Object> afterState) {
        evidenceLedger.append(new EvidenceEvent(
                UUID.randomUUID().toString(), tenantId, caseId, eventType,
                ActorType.SYSTEM, "kanon-annotation-orchestrator",
                "annotation-task-orchestrator", null, null,
                beforeState, afterState, "Annotation task orchestration", Instant.now()
        ));
    }
}

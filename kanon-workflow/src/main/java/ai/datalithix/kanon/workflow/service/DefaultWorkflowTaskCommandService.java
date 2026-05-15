package ai.datalithix.kanon.workflow.service;

import ai.datalithix.kanon.common.ActorType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.evidence.model.EvidenceEvent;
import ai.datalithix.kanon.evidence.service.EvidenceLedger;
import ai.datalithix.kanon.workflow.model.ApprovalStatus;
import ai.datalithix.kanon.workflow.model.ReviewStatus;
import ai.datalithix.kanon.workflow.model.WorkflowActionRequest;
import ai.datalithix.kanon.workflow.model.WorkflowInstance;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DefaultWorkflowTaskCommandService implements WorkflowTaskCommandService {
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final EvidenceLedger evidenceLedger;

    public DefaultWorkflowTaskCommandService(
            WorkflowInstanceRepository workflowInstanceRepository,
            EvidenceLedger evidenceLedger
    ) {
        this.workflowInstanceRepository = workflowInstanceRepository;
        this.evidenceLedger = evidenceLedger;
    }

    @Override
    public WorkflowInstance startReview(WorkflowActionRequest request) {
        WorkflowInstance before = requireInstance(request);
        WorkflowInstance after = copy(
                before,
                "HUMAN_REVIEW",
                "IN_REVIEW",
                null,
                null,
                null,
                true,
                ReviewStatus.IN_REVIEW,
                request.actorId(),
                before.approvalStatus() == ApprovalStatus.NOT_REQUIRED ? ApprovalStatus.PENDING : before.approvalStatus(),
                before.approvedBy(),
                before.approvedAt(),
                before.escalationReason(),
                before.exportReady(),
                request.actorId(),
                null
        );
        return saveWithEvidence(before, after, "REVIEW_STARTED", request);
    }

    @Override
    public WorkflowInstance completeReview(WorkflowActionRequest request) {
        WorkflowInstance before = requireInstance(request);
        WorkflowInstance after = copy(
                before,
                "HUMAN_REVIEW",
                "REVIEW_COMPLETED",
                null,
                null,
                null,
                before.reviewRequired(),
                ReviewStatus.COMPLETED,
                request.actorId(),
                before.approvalStatus() == ApprovalStatus.NOT_REQUIRED ? ApprovalStatus.PENDING : before.approvalStatus(),
                before.approvedBy(),
                before.approvedAt(),
                before.escalationReason(),
                before.exportReady(),
                request.actorId(),
                null
        );
        return saveWithEvidence(before, after, "REVIEW_COMPLETED", request);
    }

    @Override
    public WorkflowInstance approve(WorkflowActionRequest request) {
        WorkflowInstance before = requireInstance(request);
        Instant now = Instant.now();
        WorkflowInstance after = copy(
                before,
                "APPROVAL",
                "APPROVED",
                null,
                null,
                null,
                before.reviewRequired(),
                before.reviewStatus() == ReviewStatus.PENDING ? ReviewStatus.COMPLETED : before.reviewStatus(),
                before.reviewerId(),
                ApprovalStatus.APPROVED,
                request.actorId(),
                now,
                before.escalationReason(),
                true,
                request.actorId(),
                now
        );
        return saveWithEvidence(before, after, "APPROVED", request);
    }

    @Override
    public WorkflowInstance reject(WorkflowActionRequest request) {
        WorkflowInstance before = requireInstance(request);
        Instant now = Instant.now();
        WorkflowInstance after = copy(
                before,
                "APPROVAL",
                "REJECTED",
                now,
                null,
                request.reason(),
                before.reviewRequired(),
                before.reviewStatus(),
                before.reviewerId(),
                ApprovalStatus.REJECTED,
                before.approvedBy(),
                before.approvedAt(),
                before.escalationReason(),
                false,
                request.actorId(),
                now
        );
        return saveWithEvidence(before, after, "REJECTED", request);
    }

    @Override
    public WorkflowInstance escalate(WorkflowActionRequest request) {
        WorkflowInstance before = requireInstance(request);
        WorkflowInstance after = copy(
                before,
                "ESCALATION",
                "ESCALATED",
                null,
                null,
                null,
                true,
                ReviewStatus.ESCALATED,
                before.reviewerId(),
                ApprovalStatus.ESCALATED,
                before.approvedBy(),
                before.approvedAt(),
                request.reason(),
                false,
                request.actorId(),
                null
        );
        return saveWithEvidence(before, after, "ESCALATED", request);
    }

    @Override
    public WorkflowInstance markExportReady(WorkflowActionRequest request) {
        WorkflowInstance before = requireInstance(request);
        WorkflowInstance after = copy(
                before,
                "EXPORT",
                "EXPORT_READY",
                null,
                null,
                null,
                before.reviewRequired(),
                before.reviewStatus(),
                before.reviewerId(),
                before.approvalStatus(),
                before.approvedBy(),
                before.approvedAt(),
                before.escalationReason(),
                true,
                request.actorId(),
                null
        );
        return saveWithEvidence(before, after, "EXPORT_READY", request);
    }

    private WorkflowInstance requireInstance(WorkflowActionRequest request) {
        return workflowInstanceRepository.findById(request.tenantId(), request.workflowInstanceId())
                .orElseThrow(() -> new IllegalArgumentException("Workflow instance not found: " + request.workflowInstanceId()));
    }

    private WorkflowInstance saveWithEvidence(
            WorkflowInstance before,
            WorkflowInstance afterWithoutEvidence,
            String eventType,
            WorkflowActionRequest request
    ) {
        Instant now = Instant.now();
        String eventId = "evt-" + UUID.randomUUID();
        EvidenceEvent event = new EvidenceEvent(
                eventId,
                before.tenantId(),
                before.caseId(),
                eventType,
                ActorType.HUMAN,
                request.actorId(),
                null,
                null,
                null,
                state(before),
                state(afterWithoutEvidence),
                request.reason(),
                now
        );
        evidenceLedger.append(event);
        WorkflowInstance after = withEvidenceEvent(afterWithoutEvidence, eventId, request.actorId(), now);
        return workflowInstanceRepository.save(after);
    }

    private static WorkflowInstance withEvidenceEvent(WorkflowInstance workflowInstance, String eventId, String actorId, Instant now) {
        List<String> eventIds = new ArrayList<>(workflowInstance.evidenceEventIds());
        eventIds.add(eventId);
        return new WorkflowInstance(
                workflowInstance.workflowInstanceId(),
                workflowInstance.workflowId(),
                workflowInstance.tenantId(),
                workflowInstance.organizationId(),
                workflowInstance.workspaceId(),
                workflowInstance.caseId(),
                workflowInstance.mediaAssetId(),
                workflowInstance.currentStep(),
                workflowInstance.currentState(),
                workflowInstance.assignedAgentId(),
                workflowInstance.assignedUserId(),
                workflowInstance.assignedMembershipId(),
                workflowInstance.priority(),
                workflowInstance.dueAt(),
                workflowInstance.startedAt(),
                workflowInstance.completedAt(),
                workflowInstance.failedAt(),
                workflowInstance.failureReason(),
                workflowInstance.reviewRequired(),
                workflowInstance.reviewStatus(),
                workflowInstance.reviewerId(),
                workflowInstance.reviewerMembershipId(),
                workflowInstance.approvalStatus(),
                workflowInstance.approvedBy(),
                workflowInstance.approverMembershipId(),
                workflowInstance.approvedAt(),
                workflowInstance.escalationReason(),
                workflowInstance.exportReady(),
                eventIds,
                workflowInstance.traceId(),
                workflowInstance.correlationId(),
                workflowInstance.modelInvocationIds(),
                workflowInstance.inputAssetIds(),
                workflowInstance.outputAssetIds(),
                audit(workflowInstance.audit(), actorId, now)
        );
    }

    private static WorkflowInstance copy(
            WorkflowInstance workflowInstance,
            String currentStep,
            String currentState,
            Instant completedAt,
            Instant failedAt,
            String failureReason,
            boolean reviewRequired,
            ReviewStatus reviewStatus,
            String reviewerId,
            ApprovalStatus approvalStatus,
            String approvedBy,
            Instant approvedAt,
            String escalationReason,
            boolean exportReady,
            String actorId,
            Instant completedAtFallback
    ) {
        Instant now = Instant.now();
        return new WorkflowInstance(
                workflowInstance.workflowInstanceId(),
                workflowInstance.workflowId(),
                workflowInstance.tenantId(),
                workflowInstance.organizationId(),
                workflowInstance.workspaceId(),
                workflowInstance.caseId(),
                workflowInstance.mediaAssetId(),
                currentStep,
                currentState,
                workflowInstance.assignedAgentId(),
                workflowInstance.assignedUserId(),
                workflowInstance.assignedMembershipId(),
                workflowInstance.priority(),
                workflowInstance.dueAt(),
                workflowInstance.startedAt(),
                completedAt == null ? completedAtFallback : completedAt,
                failedAt,
                failureReason,
                reviewRequired,
                reviewStatus,
                reviewerId,
                workflowInstance.reviewerMembershipId(),
                approvalStatus,
                approvedBy,
                workflowInstance.approverMembershipId(),
                approvedAt,
                escalationReason,
                exportReady,
                workflowInstance.evidenceEventIds(),
                workflowInstance.traceId(),
                workflowInstance.correlationId(),
                workflowInstance.modelInvocationIds(),
                workflowInstance.inputAssetIds(),
                workflowInstance.outputAssetIds(),
                audit(workflowInstance.audit(), actorId, now)
        );
    }

    private static AuditMetadata audit(AuditMetadata previous, String actorId, Instant now) {
        return new AuditMetadata(
                previous.createdAt(),
                previous.createdBy(),
                now,
                actorId,
                previous.version() + 1
        );
    }

    private static Map<String, Object> state(WorkflowInstance workflowInstance) {
        return Map.of(
                "workflowInstanceId", workflowInstance.workflowInstanceId(),
                "currentState", value(workflowInstance.currentState()),
                "reviewStatus", workflowInstance.reviewStatus().name(),
                "approvalStatus", workflowInstance.approvalStatus().name(),
                "exportReady", workflowInstance.exportReady()
        );
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }
}

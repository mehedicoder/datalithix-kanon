package ai.datalithix.kanon.workflow.service;

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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultWorkflowTaskCommandServiceTest {
    @Test
    void approveUpdatesWorkflowAndAppendsEvidence() {
        InMemoryWorkflowInstanceRepository repository = new InMemoryWorkflowInstanceRepository();
        CapturingEvidenceLedger ledger = new CapturingEvidenceLedger();
        DefaultWorkflowTaskCommandService service = new DefaultWorkflowTaskCommandService(repository, ledger);
        repository.save(instance());

        WorkflowInstance approved = service.approve(request("approval accepted"));

        assertEquals(ApprovalStatus.APPROVED, approved.approvalStatus());
        assertEquals("reviewer-1", approved.approvedBy());
        assertTrue(approved.exportReady());
        assertFalse(approved.evidenceEventIds().isEmpty());
        assertEquals("APPROVED", ledger.events().getFirst().eventType());
        assertEquals("approval accepted", ledger.events().getFirst().rationale());
    }

    @Test
    void escalateKeepsTaskAuditableAndNotExportReady() {
        InMemoryWorkflowInstanceRepository repository = new InMemoryWorkflowInstanceRepository();
        CapturingEvidenceLedger ledger = new CapturingEvidenceLedger();
        DefaultWorkflowTaskCommandService service = new DefaultWorkflowTaskCommandService(repository, ledger);
        repository.save(instance());

        WorkflowInstance escalated = service.escalate(request("needs domain manager"));

        assertEquals(ReviewStatus.ESCALATED, escalated.reviewStatus());
        assertEquals(ApprovalStatus.ESCALATED, escalated.approvalStatus());
        assertEquals("needs domain manager", escalated.escalationReason());
        assertFalse(escalated.exportReady());
        assertEquals("ESCALATED", ledger.events().getFirst().eventType());
    }

    private static WorkflowActionRequest request(String reason) {
        return new WorkflowActionRequest("tenant-a", "workflow-instance-1", "reviewer-1", reason);
    }

    private static WorkflowInstance instance() {
        Instant now = Instant.parse("2026-04-17T00:00:00Z");
        return new WorkflowInstance(
                "workflow-instance-1",
                "workflow-1",
                "tenant-a",
                "case-1",
                null,
                "HUMAN_REVIEW",
                "PENDING_REVIEW",
                "agent-1",
                "reviewer-1",
                5,
                now.plusSeconds(3600),
                now,
                null,
                null,
                null,
                true,
                ReviewStatus.PENDING,
                null,
                ApprovalStatus.PENDING,
                null,
                null,
                null,
                false,
                List.of(),
                "trace-1",
                "correlation-1",
                List.of(),
                List.of("asset-in"),
                List.of(),
                new AuditMetadata(now, "system", now, "system", 1)
        );
    }

    private static class CapturingEvidenceLedger implements EvidenceLedger {
        private final List<EvidenceEvent> events = new ArrayList<>();

        @Override
        public void append(EvidenceEvent event) {
            events.add(event);
        }

        List<EvidenceEvent> events() {
            return events;
        }
    }
}

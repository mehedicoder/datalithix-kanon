package ai.datalithix.kanon.bootstrap.persistence;

import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.workflow.model.ApprovalStatus;
import ai.datalithix.kanon.workflow.model.ReviewStatus;
import ai.datalithix.kanon.workflow.model.WorkflowInstance;
import ai.datalithix.kanon.workflow.service.WorkflowInstanceRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.audit;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.instant;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.joinStrings;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.stringList;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.timestamp;

@Repository
@Profile("!test")
public class PostgresWorkflowInstanceRepository implements WorkflowInstanceRepository {
    private final JdbcTemplate jdbcTemplate;

    public PostgresWorkflowInstanceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public WorkflowInstance save(WorkflowInstance workflowInstance) {
        jdbcTemplate.update("""
                        INSERT INTO workflow_instance (
                            workflow_instance_id, workflow_id, tenant_id, organization_id, workspace_id, case_id, media_asset_id, current_step,
                            current_state, assigned_agent_id, assigned_user_id, assigned_membership_id, priority, due_at, started_at,
                            completed_at, failed_at, failure_reason, review_required, review_status, reviewer_id, reviewer_membership_id,
                            approval_status, approved_by, approver_membership_id, approved_at, escalation_reason, export_ready,
                            evidence_event_ids, trace_id, correlation_id, model_invocation_ids, input_asset_ids,
                            output_asset_ids, created_at, created_by, updated_at, updated_by, audit_version
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (tenant_id, workflow_instance_id)
                        DO UPDATE SET
                            workflow_id = EXCLUDED.workflow_id,
                            organization_id = EXCLUDED.organization_id,
                            workspace_id = EXCLUDED.workspace_id,
                            case_id = EXCLUDED.case_id,
                            media_asset_id = EXCLUDED.media_asset_id,
                            current_step = EXCLUDED.current_step,
                            current_state = EXCLUDED.current_state,
                            assigned_agent_id = EXCLUDED.assigned_agent_id,
                            assigned_user_id = EXCLUDED.assigned_user_id,
                            assigned_membership_id = EXCLUDED.assigned_membership_id,
                            priority = EXCLUDED.priority,
                            due_at = EXCLUDED.due_at,
                            started_at = EXCLUDED.started_at,
                            completed_at = EXCLUDED.completed_at,
                            failed_at = EXCLUDED.failed_at,
                            failure_reason = EXCLUDED.failure_reason,
                            review_required = EXCLUDED.review_required,
                            review_status = EXCLUDED.review_status,
                            reviewer_id = EXCLUDED.reviewer_id,
                            reviewer_membership_id = EXCLUDED.reviewer_membership_id,
                            approval_status = EXCLUDED.approval_status,
                            approved_by = EXCLUDED.approved_by,
                            approver_membership_id = EXCLUDED.approver_membership_id,
                            approved_at = EXCLUDED.approved_at,
                            escalation_reason = EXCLUDED.escalation_reason,
                            export_ready = EXCLUDED.export_ready,
                            evidence_event_ids = EXCLUDED.evidence_event_ids,
                            trace_id = EXCLUDED.trace_id,
                            correlation_id = EXCLUDED.correlation_id,
                            model_invocation_ids = EXCLUDED.model_invocation_ids,
                            input_asset_ids = EXCLUDED.input_asset_ids,
                            output_asset_ids = EXCLUDED.output_asset_ids,
                            updated_at = EXCLUDED.updated_at,
                            updated_by = EXCLUDED.updated_by,
                            audit_version = workflow_instance.audit_version + 1
                        """,
                workflowInstance.workflowInstanceId(), workflowInstance.workflowId(), workflowInstance.tenantId(),
                workflowInstance.organizationId(), workflowInstance.workspaceId(), workflowInstance.caseId(),
                workflowInstance.mediaAssetId(), workflowInstance.currentStep(), workflowInstance.currentState(),
                workflowInstance.assignedAgentId(), workflowInstance.assignedUserId(), workflowInstance.assignedMembershipId(),
                workflowInstance.priority(), timestamp(workflowInstance.dueAt()), timestamp(workflowInstance.startedAt()),
                timestamp(workflowInstance.completedAt()), timestamp(workflowInstance.failedAt()), workflowInstance.failureReason(),
                workflowInstance.reviewRequired(), workflowInstance.reviewStatus().name(), workflowInstance.reviewerId(),
                workflowInstance.reviewerMembershipId(), workflowInstance.approvalStatus().name(), workflowInstance.approvedBy(),
                workflowInstance.approverMembershipId(), timestamp(workflowInstance.approvedAt()),
                workflowInstance.escalationReason(), workflowInstance.exportReady(), joinStrings(workflowInstance.evidenceEventIds()),
                workflowInstance.traceId(), workflowInstance.correlationId(), joinStrings(workflowInstance.modelInvocationIds()),
                joinStrings(workflowInstance.inputAssetIds()), joinStrings(workflowInstance.outputAssetIds()),
                timestamp(workflowInstance.audit().createdAt()), workflowInstance.audit().createdBy(),
                timestamp(workflowInstance.audit().updatedAt()), workflowInstance.audit().updatedBy(),
                workflowInstance.audit().version()
        );
        return findById(workflowInstance.tenantId(), workflowInstance.workflowInstanceId()).orElse(workflowInstance);
    }

    @Override
    public Optional<WorkflowInstance> findById(String tenantId, String workflowInstanceId) {
        return jdbcTemplate.query("SELECT * FROM workflow_instance WHERE tenant_id = ? AND workflow_instance_id = ?",
                this::map, tenantId, workflowInstanceId).stream().findFirst();
    }

    @Override
    public List<WorkflowInstance> findByCaseId(String tenantId, String caseId) {
        return jdbcTemplate.query("""
                        SELECT * FROM workflow_instance
                        WHERE tenant_id = ? AND case_id = ?
                        ORDER BY started_at ASC NULLS LAST, updated_at ASC
                        """,
                this::map,
                tenantId,
                caseId
        );
    }

    @Override
    public List<WorkflowInstance> findOpenReviewTasks(String tenantId, String assignedUserId) {
        if (assignedUserId == null || assignedUserId.isBlank()) {
            return jdbcTemplate.query("""
                            SELECT * FROM workflow_instance
                            WHERE tenant_id = ?
                              AND review_required = TRUE
                              AND completed_at IS NULL
                              AND failed_at IS NULL
                            ORDER BY due_at ASC NULLS LAST, updated_at ASC
                            """,
                    this::map,
                    tenantId
            );
        }
        return jdbcTemplate.query("""
                        SELECT * FROM workflow_instance
                        WHERE tenant_id = ?
                          AND assigned_user_id = ?
                          AND review_required = TRUE
                          AND completed_at IS NULL
                          AND failed_at IS NULL
                        ORDER BY due_at ASC NULLS LAST, updated_at ASC
                        """,
                this::map,
                tenantId,
                assignedUserId
        );
    }

    @Override
    public PageResult<WorkflowInstance> findPage(QuerySpec query) {
        int limit = query.page().pageSize();
        int offset = query.page().pageNumber() * query.page().pageSize();
        String organizationId = query.dimensions().get("organizationId");
        String workspaceId = query.dimensions().get("workspaceId");
        String reviewStatus = query.dimensions().get("reviewStatus");
        if (organizationId != null && workspaceId != null && reviewStatus != null) {
            List<WorkflowInstance> items = jdbcTemplate.query("""
                            SELECT * FROM workflow_instance
                            WHERE tenant_id = ? AND organization_id = ? AND workspace_id = ? AND review_status = ?
                            ORDER BY due_at ASC NULLS LAST, updated_at ASC
                            LIMIT ? OFFSET ?
                            """,
                    this::map,
                    query.tenantId(),
                    organizationId,
                    workspaceId,
                    reviewStatus,
                    limit,
                    offset
            );
            Long total = jdbcTemplate.queryForObject("""
                            SELECT COUNT(*) FROM workflow_instance
                            WHERE tenant_id = ? AND organization_id = ? AND workspace_id = ? AND review_status = ?
                            """,
                    Long.class,
                    query.tenantId(),
                    organizationId,
                    workspaceId,
                    reviewStatus
            );
            return new PageResult<>(items, query.page().pageNumber(), query.page().pageSize(), total == null ? 0 : total);
        }
        if (organizationId != null && workspaceId != null) {
            List<WorkflowInstance> items = jdbcTemplate.query("""
                            SELECT * FROM workflow_instance
                            WHERE tenant_id = ? AND organization_id = ? AND workspace_id = ?
                            ORDER BY updated_at DESC, workflow_instance_id ASC
                            LIMIT ? OFFSET ?
                            """,
                    this::map,
                    query.tenantId(),
                    organizationId,
                    workspaceId,
                    limit,
                    offset
            );
            Long total = jdbcTemplate.queryForObject("""
                            SELECT COUNT(*) FROM workflow_instance
                            WHERE tenant_id = ? AND organization_id = ? AND workspace_id = ?
                            """,
                    Long.class,
                    query.tenantId(),
                    organizationId,
                    workspaceId
            );
            return new PageResult<>(items, query.page().pageNumber(), query.page().pageSize(), total == null ? 0 : total);
        }
        List<WorkflowInstance> items = jdbcTemplate.query("""
                        SELECT * FROM workflow_instance
                        WHERE tenant_id = ?
                        ORDER BY updated_at DESC, workflow_instance_id ASC
                        LIMIT ? OFFSET ?
                        """,
                this::map,
                query.tenantId(),
                limit,
                offset
        );
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM workflow_instance WHERE tenant_id = ?",
                Long.class,
                query.tenantId()
        );
        return new PageResult<>(items, query.page().pageNumber(), query.page().pageSize(), total == null ? 0 : total);
    }

    private WorkflowInstance map(ResultSet resultSet, int rowNumber) throws SQLException {
        return new WorkflowInstance(
                resultSet.getString("workflow_instance_id"),
                resultSet.getString("workflow_id"),
                resultSet.getString("tenant_id"),
                resultSet.getString("organization_id"),
                resultSet.getString("workspace_id"),
                resultSet.getString("case_id"),
                resultSet.getString("media_asset_id"),
                resultSet.getString("current_step"),
                resultSet.getString("current_state"),
                resultSet.getString("assigned_agent_id"),
                resultSet.getString("assigned_user_id"),
                resultSet.getString("assigned_membership_id"),
                resultSet.getInt("priority"),
                instant(resultSet, "due_at"),
                instant(resultSet, "started_at"),
                instant(resultSet, "completed_at"),
                instant(resultSet, "failed_at"),
                resultSet.getString("failure_reason"),
                resultSet.getBoolean("review_required"),
                ReviewStatus.valueOf(resultSet.getString("review_status")),
                resultSet.getString("reviewer_id"),
                resultSet.getString("reviewer_membership_id"),
                ApprovalStatus.valueOf(resultSet.getString("approval_status")),
                resultSet.getString("approved_by"),
                resultSet.getString("approver_membership_id"),
                instant(resultSet, "approved_at"),
                resultSet.getString("escalation_reason"),
                resultSet.getBoolean("export_ready"),
                stringList(resultSet.getString("evidence_event_ids")),
                resultSet.getString("trace_id"),
                resultSet.getString("correlation_id"),
                stringList(resultSet.getString("model_invocation_ids")),
                stringList(resultSet.getString("input_asset_ids")),
                stringList(resultSet.getString("output_asset_ids")),
                audit(resultSet)
        );
    }
}

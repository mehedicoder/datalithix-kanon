package ai.datalithix.kanon.bootstrap.persistence;

import ai.datalithix.kanon.common.AiTaskType;
import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DataResidency;
import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.workflow.model.PlannerType;
import ai.datalithix.kanon.workflow.model.WorkflowDefinition;
import ai.datalithix.kanon.workflow.model.WorkflowStatus;
import ai.datalithix.kanon.workflow.model.WorkflowType;
import ai.datalithix.kanon.workflow.service.WorkflowDefinitionRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.audit;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.joinStrings;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.optionalEnum;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.stringList;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.stringSet;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.timestamp;

@Repository
@Profile("!test")
public class PostgresWorkflowDefinitionRepository implements WorkflowDefinitionRepository {
    private final JdbcTemplate jdbcTemplate;

    public PostgresWorkflowDefinitionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public WorkflowDefinition save(WorkflowDefinition workflowDefinition) {
        jdbcTemplate.update("""
                        INSERT INTO workflow_definition (
                            workflow_id, tenant_id, organization_id, workspace_id, name, workflow_type, description, status, enabled, domain_type,
                            task_type, asset_type, source_type, policy_profile, regulatory_act, data_residency, goal,
                            planner_type, planner_version, action_set_ref, preconditions, constraints_text,
                            fallback_workflow_ref, model_route_policy, allowed_model_profile_ids,
                            created_at, created_by, updated_at, updated_by, audit_version
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (tenant_id, workflow_id)
                        DO UPDATE SET
                            organization_id = EXCLUDED.organization_id,
                            workspace_id = EXCLUDED.workspace_id,
                            name = EXCLUDED.name,
                            workflow_type = EXCLUDED.workflow_type,
                            description = EXCLUDED.description,
                            status = EXCLUDED.status,
                            enabled = EXCLUDED.enabled,
                            domain_type = EXCLUDED.domain_type,
                            task_type = EXCLUDED.task_type,
                            asset_type = EXCLUDED.asset_type,
                            source_type = EXCLUDED.source_type,
                            policy_profile = EXCLUDED.policy_profile,
                            regulatory_act = EXCLUDED.regulatory_act,
                            data_residency = EXCLUDED.data_residency,
                            goal = EXCLUDED.goal,
                            planner_type = EXCLUDED.planner_type,
                            planner_version = EXCLUDED.planner_version,
                            action_set_ref = EXCLUDED.action_set_ref,
                            preconditions = EXCLUDED.preconditions,
                            constraints_text = EXCLUDED.constraints_text,
                            fallback_workflow_ref = EXCLUDED.fallback_workflow_ref,
                            model_route_policy = EXCLUDED.model_route_policy,
                            allowed_model_profile_ids = EXCLUDED.allowed_model_profile_ids,
                            updated_at = EXCLUDED.updated_at,
                            updated_by = EXCLUDED.updated_by,
                            audit_version = workflow_definition.audit_version + 1
                        """,
                workflowDefinition.workflowId(), workflowDefinition.tenantId(),
                workflowDefinition.organizationId(), workflowDefinition.workspaceId(), workflowDefinition.name(),
                workflowDefinition.workflowType().name(), workflowDefinition.description(), workflowDefinition.status().name(),
                workflowDefinition.enabled(), name(workflowDefinition.domainType()), name(workflowDefinition.taskType()),
                name(workflowDefinition.assetType()), name(workflowDefinition.sourceType()), workflowDefinition.policyProfile(),
                workflowDefinition.regulatoryAct(), name(workflowDefinition.dataResidency()), workflowDefinition.goal(),
                workflowDefinition.plannerType().name(), workflowDefinition.plannerVersion(), workflowDefinition.actionSetRef(),
                joinStrings(workflowDefinition.preconditions()), joinStrings(workflowDefinition.constraints()),
                workflowDefinition.fallbackWorkflowRef(), workflowDefinition.modelRoutePolicy(),
                joinStrings(workflowDefinition.allowedModelProfileIds()), timestamp(workflowDefinition.audit().createdAt()),
                workflowDefinition.audit().createdBy(), timestamp(workflowDefinition.audit().updatedAt()),
                workflowDefinition.audit().updatedBy(), workflowDefinition.audit().version()
        );
        return findById(workflowDefinition.tenantId(), workflowDefinition.workflowId()).orElse(workflowDefinition);
    }

    @Override
    public Optional<WorkflowDefinition> findById(String tenantId, String workflowId) {
        return jdbcTemplate.query("SELECT * FROM workflow_definition WHERE tenant_id = ? AND workflow_id = ?",
                this::map, tenantId, workflowId).stream().findFirst();
    }

    @Override
    public void deleteById(String tenantId, String workflowId) {
        jdbcTemplate.update(
                "DELETE FROM workflow_definition WHERE tenant_id = ? AND workflow_id = ?",
                tenantId,
                workflowId
        );
    }

    @Override
    public List<WorkflowDefinition> findEnabledByTenant(String tenantId) {
        return jdbcTemplate.query("""
                        SELECT * FROM workflow_definition
                        WHERE tenant_id = ? AND enabled = TRUE
                        ORDER BY name ASC
                        """,
                this::map,
                tenantId
        );
    }

    @Override
    public List<WorkflowDefinition> findEnabledByType(String tenantId, WorkflowType workflowType) {
        return jdbcTemplate.query("""
                        SELECT * FROM workflow_definition
                        WHERE tenant_id = ? AND workflow_type = ? AND enabled = TRUE
                        ORDER BY name ASC
                        """,
                this::map,
                tenantId,
                workflowType.name()
        );
    }

    @Override
    public PageResult<WorkflowDefinition> findPage(QuerySpec query) {
        int limit = query.page().pageSize();
        int offset = query.page().pageNumber() * query.page().pageSize();
        String organizationId = query.dimensions().get("organizationId");
        String workspaceId = query.dimensions().get("workspaceId");
        if (organizationId != null && workspaceId != null) {
            List<WorkflowDefinition> items = jdbcTemplate.query("""
                            SELECT * FROM workflow_definition
                            WHERE tenant_id = ? AND organization_id = ? AND workspace_id = ?
                            ORDER BY updated_at DESC, name ASC
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
                            SELECT COUNT(*) FROM workflow_definition
                            WHERE tenant_id = ? AND organization_id = ? AND workspace_id = ?
                            """,
                    Long.class,
                    query.tenantId(),
                    organizationId,
                    workspaceId
            );
            return new PageResult<>(items, query.page().pageNumber(), query.page().pageSize(), total == null ? 0 : total);
        }
        List<WorkflowDefinition> items = jdbcTemplate.query("""
                        SELECT * FROM workflow_definition
                        WHERE tenant_id = ?
                        ORDER BY updated_at DESC, name ASC
                        LIMIT ? OFFSET ?
                        """,
                this::map,
                query.tenantId(),
                limit,
                offset
        );
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM workflow_definition WHERE tenant_id = ?",
                Long.class,
                query.tenantId()
        );
        return new PageResult<>(items, query.page().pageNumber(), query.page().pageSize(), total == null ? 0 : total);
    }

    private WorkflowDefinition map(ResultSet resultSet, int rowNumber) throws SQLException {
        return new WorkflowDefinition(
                resultSet.getString("workflow_id"),
                resultSet.getString("tenant_id"),
                resultSet.getString("organization_id"),
                resultSet.getString("workspace_id"),
                resultSet.getString("name"),
                WorkflowType.valueOf(resultSet.getString("workflow_type")),
                resultSet.getString("description"),
                WorkflowStatus.valueOf(resultSet.getString("status")),
                resultSet.getBoolean("enabled"),
                optionalEnum(resultSet.getString("domain_type"), DomainType::valueOf),
                optionalEnum(resultSet.getString("task_type"), AiTaskType::valueOf),
                optionalEnum(resultSet.getString("asset_type"), AssetType::valueOf),
                optionalEnum(resultSet.getString("source_type"), SourceType::valueOf),
                resultSet.getString("policy_profile"),
                resultSet.getString("regulatory_act"),
                optionalEnum(resultSet.getString("data_residency"), DataResidency::valueOf),
                resultSet.getString("goal"),
                PlannerType.valueOf(resultSet.getString("planner_type")),
                resultSet.getString("planner_version"),
                resultSet.getString("action_set_ref"),
                stringList(resultSet.getString("preconditions")),
                stringList(resultSet.getString("constraints_text")),
                resultSet.getString("fallback_workflow_ref"),
                resultSet.getString("model_route_policy"),
                stringSet(resultSet.getString("allowed_model_profile_ids")),
                audit(resultSet)
        );
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }
}

package ai.datalithix.kanon.bootstrap.persistence;

import ai.datalithix.kanon.agentruntime.model.AgentExecutionMode;
import ai.datalithix.kanon.agentruntime.model.AgentProfile;
import ai.datalithix.kanon.agentruntime.model.AgentStatus;
import ai.datalithix.kanon.agentruntime.model.AgentType;
import ai.datalithix.kanon.agentruntime.service.AgentProfileRepository;
import ai.datalithix.kanon.common.AiTaskType;
import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.common.runtime.RetryPolicy;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.audit;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.duration;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.enumSet;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.instant;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.joinEnums;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.joinStrings;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.millis;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.stringSet;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.timestamp;

@Repository
@Profile("!test")
public class PostgresAgentProfileRepository implements AgentProfileRepository {
    private final JdbcTemplate jdbcTemplate;

    public PostgresAgentProfileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AgentProfile save(AgentProfile agentProfile) {
        RetryPolicy retry = agentProfile.retryPolicy();
        jdbcTemplate.update("""
                        INSERT INTO agent_profile (
                            agent_id, tenant_id, name, agent_type, description, status, enabled,
                            supported_domains, supported_task_types, supported_asset_types, supported_source_types,
                            supported_annotation_types, required_policies, required_permissions, input_schema_ref,
                            output_schema_ref, execution_mode, timeout_seconds, retry_max_attempts,
                            retry_initial_backoff_ms, retry_max_backoff_ms, max_attempts, concurrency_limit, priority,
                            queue_name, runtime_profile, configuration_ref, model_route_policy, preferred_model_profile,
                            fallback_model_profile, allowed_model_profile_ids, required_llm_service_capabilities,
                            allow_cloud_models, allow_local_models, max_cost_class, max_latency_class, compliance_tags,
                            evidence_required, trace_enabled, last_run_at, last_success_at, last_failure_at,
                            last_failure_reason, created_at, created_by, updated_at, updated_by, audit_version
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (tenant_id, agent_id)
                        DO UPDATE SET
                            name = EXCLUDED.name,
                            agent_type = EXCLUDED.agent_type,
                            description = EXCLUDED.description,
                            status = EXCLUDED.status,
                            enabled = EXCLUDED.enabled,
                            supported_domains = EXCLUDED.supported_domains,
                            supported_task_types = EXCLUDED.supported_task_types,
                            supported_asset_types = EXCLUDED.supported_asset_types,
                            supported_source_types = EXCLUDED.supported_source_types,
                            supported_annotation_types = EXCLUDED.supported_annotation_types,
                            required_policies = EXCLUDED.required_policies,
                            required_permissions = EXCLUDED.required_permissions,
                            input_schema_ref = EXCLUDED.input_schema_ref,
                            output_schema_ref = EXCLUDED.output_schema_ref,
                            execution_mode = EXCLUDED.execution_mode,
                            timeout_seconds = EXCLUDED.timeout_seconds,
                            retry_max_attempts = EXCLUDED.retry_max_attempts,
                            retry_initial_backoff_ms = EXCLUDED.retry_initial_backoff_ms,
                            retry_max_backoff_ms = EXCLUDED.retry_max_backoff_ms,
                            max_attempts = EXCLUDED.max_attempts,
                            concurrency_limit = EXCLUDED.concurrency_limit,
                            priority = EXCLUDED.priority,
                            queue_name = EXCLUDED.queue_name,
                            runtime_profile = EXCLUDED.runtime_profile,
                            configuration_ref = EXCLUDED.configuration_ref,
                            model_route_policy = EXCLUDED.model_route_policy,
                            preferred_model_profile = EXCLUDED.preferred_model_profile,
                            fallback_model_profile = EXCLUDED.fallback_model_profile,
                            allowed_model_profile_ids = EXCLUDED.allowed_model_profile_ids,
                            required_llm_service_capabilities = EXCLUDED.required_llm_service_capabilities,
                            allow_cloud_models = EXCLUDED.allow_cloud_models,
                            allow_local_models = EXCLUDED.allow_local_models,
                            max_cost_class = EXCLUDED.max_cost_class,
                            max_latency_class = EXCLUDED.max_latency_class,
                            compliance_tags = EXCLUDED.compliance_tags,
                            evidence_required = EXCLUDED.evidence_required,
                            trace_enabled = EXCLUDED.trace_enabled,
                            last_run_at = EXCLUDED.last_run_at,
                            last_success_at = EXCLUDED.last_success_at,
                            last_failure_at = EXCLUDED.last_failure_at,
                            last_failure_reason = EXCLUDED.last_failure_reason,
                            updated_at = EXCLUDED.updated_at,
                            updated_by = EXCLUDED.updated_by,
                            audit_version = agent_profile.audit_version + 1
                        """,
                agentProfile.agentId(), agentProfile.tenantId(), agentProfile.name(), agentProfile.agentType().name(),
                agentProfile.description(), agentProfile.status().name(), agentProfile.enabled(),
                joinEnums(agentProfile.supportedDomains()), joinEnums(agentProfile.supportedTaskTypes()),
                joinEnums(agentProfile.supportedAssetTypes()), joinEnums(agentProfile.supportedSourceTypes()),
                joinStrings(agentProfile.supportedAnnotationTypes()), joinStrings(agentProfile.requiredPolicies()),
                joinStrings(agentProfile.requiredPermissions()), agentProfile.inputSchemaRef(), agentProfile.outputSchemaRef(),
                agentProfile.executionMode().name(), agentProfile.timeoutSeconds(),
                retry == null ? null : retry.maxAttempts(), retry == null ? null : millis(retry.initialBackoff()),
                retry == null ? null : millis(retry.maxBackoff()), agentProfile.maxAttempts(), agentProfile.concurrencyLimit(),
                agentProfile.priority(), agentProfile.queueName(), agentProfile.runtimeProfile(), agentProfile.configurationRef(),
                agentProfile.modelRoutePolicy(), agentProfile.preferredModelProfile(), agentProfile.fallbackModelProfile(),
                joinStrings(agentProfile.allowedModelProfileIds()), joinStrings(agentProfile.requiredLlmServiceCapabilities()),
                agentProfile.allowCloudModels(), agentProfile.allowLocalModels(), agentProfile.maxCostClass(),
                agentProfile.maxLatencyClass(), joinStrings(agentProfile.complianceTags()), agentProfile.evidenceRequired(),
                agentProfile.traceEnabled(), timestamp(agentProfile.lastRunAt()), timestamp(agentProfile.lastSuccessAt()),
                timestamp(agentProfile.lastFailureAt()), agentProfile.lastFailureReason(), timestamp(agentProfile.audit().createdAt()),
                agentProfile.audit().createdBy(), timestamp(agentProfile.audit().updatedAt()), agentProfile.audit().updatedBy(),
                agentProfile.audit().version()
        );
        return findById(agentProfile.tenantId(), agentProfile.agentId()).orElse(agentProfile);
    }

    @Override
    public Optional<AgentProfile> findById(String tenantId, String agentId) {
        return jdbcTemplate.query("""
                        SELECT * FROM agent_profile
                        WHERE tenant_id = ? AND agent_id = ?
                        """,
                this::map,
                tenantId,
                agentId
        ).stream().findFirst();
    }

    public void deleteById(String tenantId, String agentId) {
        jdbcTemplate.update(
                "DELETE FROM agent_profile WHERE tenant_id = ? AND agent_id = ?",
                tenantId,
                agentId
        );
    }

    @Override
    public List<AgentProfile> findEnabledByTenant(String tenantId) {
        return jdbcTemplate.query("""
                        SELECT * FROM agent_profile
                        WHERE tenant_id = ? AND enabled = TRUE
                        ORDER BY name ASC
                        """,
                this::map,
                tenantId
        );
    }

    @Override
    public List<AgentProfile> findEnabledByType(String tenantId, AgentType agentType) {
        return jdbcTemplate.query("""
                        SELECT * FROM agent_profile
                        WHERE tenant_id = ? AND agent_type = ? AND enabled = TRUE
                        ORDER BY name ASC
                        """,
                this::map,
                tenantId,
                agentType.name()
        );
    }

    @Override
    public PageResult<AgentProfile> findPage(QuerySpec query) {
        int limit = query.page().pageSize();
        int offset = query.page().pageNumber() * query.page().pageSize();
        List<AgentProfile> items = jdbcTemplate.query("""
                        SELECT * FROM agent_profile
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
                "SELECT COUNT(*) FROM agent_profile WHERE tenant_id = ?",
                Long.class,
                query.tenantId()
        );
        return new PageResult<>(items, query.page().pageNumber(), query.page().pageSize(), total == null ? 0 : total);
    }

    private AgentProfile map(ResultSet resultSet, int rowNumber) throws SQLException {
        Integer retryMaxAttempts = (Integer) resultSet.getObject("retry_max_attempts");
        Long retryInitialBackoffMs = (Long) resultSet.getObject("retry_initial_backoff_ms");
        Long retryMaxBackoffMs = (Long) resultSet.getObject("retry_max_backoff_ms");
        RetryPolicy retryPolicy = retryMaxAttempts == null
                ? null
                : new RetryPolicy(retryMaxAttempts, duration(retryInitialBackoffMs), duration(retryMaxBackoffMs));
        return new AgentProfile(
                resultSet.getString("agent_id"),
                resultSet.getString("tenant_id"),
                resultSet.getString("name"),
                AgentType.valueOf(resultSet.getString("agent_type")),
                resultSet.getString("description"),
                AgentStatus.valueOf(resultSet.getString("status")),
                resultSet.getBoolean("enabled"),
                enumSet(resultSet.getString("supported_domains"), DomainType::valueOf),
                enumSet(resultSet.getString("supported_task_types"), AiTaskType::valueOf),
                enumSet(resultSet.getString("supported_asset_types"), AssetType::valueOf),
                enumSet(resultSet.getString("supported_source_types"), SourceType::valueOf),
                stringSet(resultSet.getString("supported_annotation_types")),
                stringSet(resultSet.getString("required_policies")),
                stringSet(resultSet.getString("required_permissions")),
                resultSet.getString("input_schema_ref"),
                resultSet.getString("output_schema_ref"),
                AgentExecutionMode.valueOf(resultSet.getString("execution_mode")),
                resultSet.getInt("timeout_seconds"),
                retryPolicy,
                resultSet.getInt("max_attempts"),
                resultSet.getInt("concurrency_limit"),
                resultSet.getInt("priority"),
                resultSet.getString("queue_name"),
                resultSet.getString("runtime_profile"),
                resultSet.getString("configuration_ref"),
                resultSet.getString("model_route_policy"),
                resultSet.getString("preferred_model_profile"),
                resultSet.getString("fallback_model_profile"),
                stringSet(resultSet.getString("allowed_model_profile_ids")),
                stringSet(resultSet.getString("required_llm_service_capabilities")),
                resultSet.getBoolean("allow_cloud_models"),
                resultSet.getBoolean("allow_local_models"),
                resultSet.getString("max_cost_class"),
                resultSet.getString("max_latency_class"),
                stringSet(resultSet.getString("compliance_tags")),
                resultSet.getBoolean("evidence_required"),
                resultSet.getBoolean("trace_enabled"),
                instant(resultSet, "last_run_at"),
                instant(resultSet, "last_success_at"),
                instant(resultSet, "last_failure_at"),
                resultSet.getString("last_failure_reason"),
                audit(resultSet)
        );
    }
}

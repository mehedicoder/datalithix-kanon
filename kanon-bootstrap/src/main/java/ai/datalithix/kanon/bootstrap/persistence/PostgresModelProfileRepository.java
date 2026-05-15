 package ai.datalithix.kanon.bootstrap.persistence;

import ai.datalithix.kanon.airouting.model.ModelExecutionPolicy;
import ai.datalithix.kanon.airouting.model.ModelProfile;
import ai.datalithix.kanon.airouting.service.ModelProfileRepository;
import ai.datalithix.kanon.common.AiTaskType;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.common.runtime.ExecutionControls;
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
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.joinEnums;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.joinStrings;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.millis;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.stringSet;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.timestamp;

@Repository
@Profile("!test")
public class PostgresModelProfileRepository implements ModelProfileRepository {
    private final JdbcTemplate jdbcTemplate;

    public PostgresModelProfileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ModelProfile save(ModelProfile modelProfile) {
        RetryPolicy retry = modelProfile.executionPolicy().retryPolicy();
        ExecutionControls controls = modelProfile.executionPolicy().executionControls();
        
        // Extract tenant ID from audit metadata's createdBy field (format: "user@tenant")
        // or use a default if not available
        String tenantId = extractTenantId(modelProfile.audit().createdBy());
        
        jdbcTemplate.update("""
                INSERT INTO model_profile (
                    profile_key, tenant_id, provider, backend_type, model_id, model_name, base_url,
                    is_local, supports_tools, supports_structured_output, task_capabilities,
                    cost_class, latency_class, locality, compliance_tags, enabled, health_status,
                    secret_ref, priority, fallback_profile_key, async_required, health_check_required,
                    evidence_required, timeout_ms, max_attempts, concurrency_limit, rate_limit_per_minute,
                    retry_max_attempts, retry_initial_backoff_ms, retry_max_backoff_ms,
                    created_at, created_by, updated_at, updated_by, audit_version
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, profile_key)
                DO UPDATE SET
                    provider = EXCLUDED.provider,
                    backend_type = EXCLUDED.backend_type,
                    model_id = EXCLUDED.model_id,
                    model_name = EXCLUDED.model_name,
                    base_url = EXCLUDED.base_url,
                    is_local = EXCLUDED.is_local,
                    supports_tools = EXCLUDED.supports_tools,
                    supports_structured_output = EXCLUDED.supports_structured_output,
                    task_capabilities = EXCLUDED.task_capabilities,
                    cost_class = EXCLUDED.cost_class,
                    latency_class = EXCLUDED.latency_class,
                    locality = EXCLUDED.locality,
                    compliance_tags = EXCLUDED.compliance_tags,
                    enabled = EXCLUDED.enabled,
                    health_status = EXCLUDED.health_status,
                    secret_ref = EXCLUDED.secret_ref,
                    priority = EXCLUDED.priority,
                    fallback_profile_key = EXCLUDED.fallback_profile_key,
                    async_required = EXCLUDED.async_required,
                    health_check_required = EXCLUDED.health_check_required,
                    evidence_required = EXCLUDED.evidence_required,
                    timeout_ms = EXCLUDED.timeout_ms,
                    max_attempts = EXCLUDED.max_attempts,
                    concurrency_limit = EXCLUDED.concurrency_limit,
                    rate_limit_per_minute = EXCLUDED.rate_limit_per_minute,
                    retry_max_attempts = EXCLUDED.retry_max_attempts,
                    retry_initial_backoff_ms = EXCLUDED.retry_initial_backoff_ms,
                    retry_max_backoff_ms = EXCLUDED.retry_max_backoff_ms,
                    updated_at = EXCLUDED.updated_at,
                    updated_by = EXCLUDED.updated_by,
                    audit_version = model_profile.audit_version + 1
                """,
                modelProfile.profileKey(),
                tenantId,
                modelProfile.provider(),
                modelProfile.backendType(),
                modelProfile.modelId(),
                modelProfile.modelName(),
                modelProfile.baseUrl(),
                modelProfile.local(),
                modelProfile.supportsTools(),
                modelProfile.supportsStructuredOutput(),
                joinEnums(modelProfile.taskCapabilities()),
                modelProfile.costClass(),
                modelProfile.latencyClass(),
                modelProfile.locality(),
                joinStrings(modelProfile.complianceTags()),
                modelProfile.enabled(),
                modelProfile.healthStatus(),
                modelProfile.secretRef(),
                modelProfile.priority(),
                modelProfile.executionPolicy().fallbackProfileKey(),
                modelProfile.executionPolicy().asyncRequired(),
                modelProfile.executionPolicy().healthCheckRequired(),
                modelProfile.executionPolicy().evidenceRequired(),
                millis(controls.timeout()),
                controls.maxAttempts(),
                controls.concurrencyLimit(),
                controls.rateLimitPerMinute(),
                retry.maxAttempts(),
                millis(retry.initialBackoff()),
                millis(retry.maxBackoff()),
                timestamp(modelProfile.audit().createdAt()),
                modelProfile.audit().createdBy(),
                timestamp(modelProfile.audit().updatedAt()),
                modelProfile.audit().updatedBy(),
                modelProfile.audit().version()
        );
        return findByProfileKey(tenantId, modelProfile.profileKey()).orElse(modelProfile);
    }
    
    private String extractTenantId(String createdBy) {
        // If createdBy is in format "user@tenant", extract tenant
        if (createdBy != null && createdBy.contains("@")) {
            String[] parts = createdBy.split("@");
            if (parts.length == 2) {
                return parts[1];
            }
        }
        // Default fallback - matches the actual tenant ID in the database
        return "default";
    }

    @Override
    public Optional<ModelProfile> findByProfileKey(String tenantId, String profileKey) {
        return jdbcTemplate.query("""
                SELECT * FROM model_profile
                WHERE tenant_id = ? AND profile_key = ?
                """,
                this::map,
                tenantId,
                profileKey
        ).stream().findFirst();
    }

    @Override
    public List<ModelProfile> findEnabledByTenant(String tenantId) {
        return jdbcTemplate.query("""
                SELECT * FROM model_profile
                WHERE tenant_id = ? AND enabled = TRUE
                ORDER BY priority DESC, model_name ASC
                """,
                this::map,
                tenantId
        );
    }

    @Override
    public PageResult<ModelProfile> findPage(QuerySpec query) {
        int limit = query.page().pageSize();
        int offset = query.page().pageNumber() * query.page().pageSize();
        List<ModelProfile> items = jdbcTemplate.query("""
                SELECT * FROM model_profile
                WHERE tenant_id = ?
                ORDER BY updated_at DESC, model_name ASC
                LIMIT ? OFFSET ?
                """,
                this::map,
                query.tenantId(),
                limit,
                offset
        );
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM model_profile WHERE tenant_id = ?",
                Long.class,
                query.tenantId()
        );
        return new PageResult<>(items, query.page().pageNumber(), query.page().pageSize(), total == null ? 0 : total);
    }

    private ModelProfile map(ResultSet rs, int rowNumber) throws SQLException {
        Integer retryMaxAttempts = (Integer) rs.getObject("retry_max_attempts");
        Long retryInitialBackoffMs = (Long) rs.getObject("retry_initial_backoff_ms");
        Long retryMaxBackoffMs = (Long) rs.getObject("retry_max_backoff_ms");
        
        RetryPolicy retryPolicy = new RetryPolicy(
                retryMaxAttempts != null ? retryMaxAttempts : 3,
                duration(retryInitialBackoffMs != null ? retryInitialBackoffMs : 1000L),
                duration(retryMaxBackoffMs != null ? retryMaxBackoffMs : 30000L)
        );
        
        Long timeoutMs = (Long) rs.getObject("timeout_ms");
        Integer maxAttempts = (Integer) rs.getObject("max_attempts");
        Integer concurrencyLimit = (Integer) rs.getObject("concurrency_limit");
        Integer rateLimitPerMinute = (Integer) rs.getObject("rate_limit_per_minute");
        
        ExecutionControls executionControls = new ExecutionControls(
                duration(timeoutMs != null ? timeoutMs : 60000L),
                maxAttempts != null ? maxAttempts : 3,
                concurrencyLimit != null ? concurrencyLimit : 10,
                rateLimitPerMinute != null ? rateLimitPerMinute : 60
        );
        
        ModelExecutionPolicy executionPolicy = new ModelExecutionPolicy(
                executionControls,
                retryPolicy,
                rs.getString("fallback_profile_key"),
                rs.getBoolean("async_required"),
                rs.getBoolean("health_check_required"),
                rs.getBoolean("evidence_required")
        );
        
        return new ModelProfile(
                rs.getString("profile_key"),
                rs.getString("provider"),
                rs.getString("backend_type"),
                rs.getString("model_id"),
                rs.getString("model_name"),
                rs.getString("base_url"),
                rs.getBoolean("is_local"),
                rs.getBoolean("supports_tools"),
                rs.getBoolean("supports_structured_output"),
                enumSet(rs.getString("task_capabilities"), AiTaskType::valueOf),
                rs.getString("cost_class"),
                rs.getString("latency_class"),
                rs.getString("locality"),
                stringSet(rs.getString("compliance_tags")),
                rs.getBoolean("enabled"),
                rs.getString("health_status"),
                rs.getString("secret_ref"),
                rs.getInt("priority"),
                executionPolicy,
                audit(rs)
        );
    }
}

// Made with Bob

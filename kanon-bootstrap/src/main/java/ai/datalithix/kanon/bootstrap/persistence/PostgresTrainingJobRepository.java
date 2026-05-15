package ai.datalithix.kanon.bootstrap.persistence;

import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.training.model.ComputeBackend;
import ai.datalithix.kanon.training.model.ComputeBackendType;
import ai.datalithix.kanon.training.model.HyperParameterConfig;
import ai.datalithix.kanon.training.model.TrainingJob;
import ai.datalithix.kanon.training.model.TrainingJobStatus;
import ai.datalithix.kanon.training.model.TrainingMetrics;
import ai.datalithix.kanon.training.service.TrainingJobRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.audit;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.fromJson;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.instant;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.timestamp;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.toJson;

@Repository
@Profile("!test")
public class PostgresTrainingJobRepository implements TrainingJobRepository {
    private final JdbcTemplate jdbcTemplate;

    public PostgresTrainingJobRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public TrainingJob save(TrainingJob job) {
        jdbcTemplate.update("""
                INSERT INTO training_job (
                    training_job_id, tenant_id, dataset_version_id, dataset_definition_id,
                    compute_backend_id, model_name, status, requested_at, started_at,
                    completed_at, failed_at, failure_reason, checkpoint_uri,
                    output_model_artifact_uri, total_duration_seconds, external_job_id,
                    hyper_parameters_json, metrics_history_json, evidence_event_ids,
                    created_at, created_by, updated_at, updated_by, audit_version
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, training_job_id)
                DO UPDATE SET
                    compute_backend_id = EXCLUDED.compute_backend_id,
                    model_name = EXCLUDED.model_name,
                    status = EXCLUDED.status,
                    started_at = EXCLUDED.started_at,
                    completed_at = EXCLUDED.completed_at,
                    failed_at = EXCLUDED.failed_at,
                    failure_reason = EXCLUDED.failure_reason,
                    checkpoint_uri = EXCLUDED.checkpoint_uri,
                    output_model_artifact_uri = EXCLUDED.output_model_artifact_uri,
                    total_duration_seconds = EXCLUDED.total_duration_seconds,
                    external_job_id = EXCLUDED.external_job_id,
                    hyper_parameters_json = EXCLUDED.hyper_parameters_json,
                    metrics_history_json = EXCLUDED.metrics_history_json,
                    evidence_event_ids = EXCLUDED.evidence_event_ids,
                    updated_at = EXCLUDED.updated_at,
                    updated_by = EXCLUDED.updated_by,
                    audit_version = training_job.audit_version + 1
                """,
                job.trainingJobId(), job.tenantId(), job.datasetVersionId(), job.datasetDefinitionId(),
                job.computeBackendId(), job.modelName(), job.status().name(),
                timestamp(job.requestedAt()), timestamp(job.startedAt()),
                timestamp(job.completedAt()), timestamp(job.failedAt()),
                job.failureReason(), job.checkpointUri(), job.outputModelArtifactUri(),
                job.totalDurationSeconds(), job.externalJobId(),
                toJson(job.hyperParameters()), toJson(job.metricsHistory()),
                toJson(job.evidenceEventIds()),
                timestamp(job.audit().createdAt()), job.audit().createdBy(),
                timestamp(job.audit().updatedAt()), job.audit().updatedBy(),
                job.audit().version()
        );
        return findById(job.tenantId(), job.trainingJobId()).orElse(job);
    }

    @Override
    public Optional<TrainingJob> findById(String tenantId, String trainingJobId) {
        return jdbcTemplate.query("SELECT * FROM training_job WHERE tenant_id = ? AND training_job_id = ?",
                this::mapJob, tenantId, trainingJobId).stream().findFirst();
    }

    @Override
    public List<TrainingJob> findByTenant(String tenantId) {
        return jdbcTemplate.query("SELECT * FROM training_job WHERE tenant_id = ? ORDER BY requested_at DESC",
                this::mapJob, tenantId);
    }

    @Override
    public List<TrainingJob> findByDatasetVersion(String tenantId, String datasetVersionId) {
        return jdbcTemplate.query("SELECT * FROM training_job WHERE tenant_id = ? AND dataset_version_id = ? ORDER BY requested_at DESC",
                this::mapJob, tenantId, datasetVersionId);
    }

    @Override
    public List<TrainingJob> findByStatus(String tenantId, String status) {
        return jdbcTemplate.query("SELECT * FROM training_job WHERE tenant_id = ? AND status = ? ORDER BY requested_at DESC",
                this::mapJob, tenantId, status);
    }

    @Override
    public void deleteById(String tenantId, String trainingJobId) {
        jdbcTemplate.update("DELETE FROM training_job WHERE tenant_id = ? AND training_job_id = ?",
                tenantId, trainingJobId);
    }

    @Override
    public ComputeBackend saveBackend(ComputeBackend backend) {
        jdbcTemplate.update("""
                INSERT INTO compute_backend (
                    backend_id, tenant_id, backend_type, name, endpoint_url, credential_ref,
                    enabled, healthy, last_health_check_at, failure_reason, configuration_json
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, backend_id)
                DO UPDATE SET
                    backend_type = EXCLUDED.backend_type,
                    name = EXCLUDED.name,
                    endpoint_url = EXCLUDED.endpoint_url,
                    credential_ref = EXCLUDED.credential_ref,
                    enabled = EXCLUDED.enabled,
                    healthy = EXCLUDED.healthy,
                    last_health_check_at = EXCLUDED.last_health_check_at,
                    failure_reason = EXCLUDED.failure_reason,
                    configuration_json = EXCLUDED.configuration_json
                """,
                backend.backendId(), backend.tenantId(), backend.backendType().name(),
                backend.name(), backend.endpointUrl(), backend.credentialRef(),
                backend.enabled(), backend.healthy(), backend.lastHealthCheckAt(),
                backend.failureReason(), toJson(backend.configuration())
        );
        return findBackendById(backend.tenantId(), backend.backendId()).orElse(backend);
    }

    @Override
    public Optional<ComputeBackend> findBackendById(String tenantId, String backendId) {
        return jdbcTemplate.query("SELECT * FROM compute_backend WHERE tenant_id = ? AND backend_id = ?",
                this::mapBackend, tenantId, backendId).stream().findFirst();
    }

    @Override
    public List<ComputeBackend> findBackendsByTenant(String tenantId) {
        return jdbcTemplate.query("SELECT * FROM compute_backend WHERE tenant_id = ? ORDER BY name ASC",
                this::mapBackend, tenantId);
    }

    @Override
    public List<ComputeBackend> findEnabledBackends(String tenantId) {
        return jdbcTemplate.query("SELECT * FROM compute_backend WHERE tenant_id = ? AND enabled = TRUE ORDER BY name ASC",
                this::mapBackend, tenantId);
    }

    @Override
    public PageResult<TrainingJob> findPage(QuerySpec query) {
        int limit = query.page().pageSize();
        int offset = query.page().pageNumber() * query.page().pageSize();
        List<TrainingJob> items = jdbcTemplate.query("""
                SELECT * FROM training_job
                WHERE tenant_id = ?
                ORDER BY requested_at DESC
                LIMIT ? OFFSET ?
                """, this::mapJob, query.tenantId(), limit, offset);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM training_job WHERE tenant_id = ?", Long.class, query.tenantId());
        return new PageResult<>(items, query.page().pageNumber(), query.page().pageSize(), total == null ? 0 : total);
    }

    private TrainingJob mapJob(ResultSet rs, int rowNum) throws SQLException {
        return new TrainingJob(
                rs.getString("training_job_id"),
                rs.getString("tenant_id"),
                rs.getString("dataset_version_id"),
                rs.getString("dataset_definition_id"),
                rs.getString("compute_backend_id"),
                rs.getString("model_name"),
                fromJson(rs.getString("hyper_parameters_json"), HyperParameterConfig.class),
                TrainingJobStatus.valueOf(rs.getString("status")),
                instant(rs, "requested_at"),
                instant(rs, "started_at"),
                instant(rs, "completed_at"),
                instant(rs, "failed_at"),
                rs.getString("failure_reason"),
                rs.getString("checkpoint_uri"),
                rs.getString("output_model_artifact_uri"),
                fromJson(rs.getString("metrics_history_json"), new TypeReference<List<TrainingMetrics>>() {}),
                rs.getLong("total_duration_seconds"),
                rs.getString("external_job_id"),
                fromJson(rs.getString("evidence_event_ids"), new TypeReference<List<String>>() {}),
                audit(rs)
        );
    }

    private ComputeBackend mapBackend(ResultSet rs, int rowNum) throws SQLException {
        return new ComputeBackend(
                rs.getString("backend_id"),
                rs.getString("tenant_id"),
                ComputeBackendType.valueOf(rs.getString("backend_type")),
                rs.getString("name"),
                rs.getString("endpoint_url"),
                rs.getString("credential_ref"),
                fromJson(rs.getString("configuration_json"), new TypeReference<Map<String, String>>() {}),
                rs.getBoolean("enabled"),
                rs.getBoolean("healthy"),
                rs.getString("last_health_check_at"),
                rs.getString("failure_reason")
        );
    }
}

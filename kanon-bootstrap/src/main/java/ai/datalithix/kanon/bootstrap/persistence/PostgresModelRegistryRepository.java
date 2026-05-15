package ai.datalithix.kanon.bootstrap.persistence;

import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.modelregistry.model.DeploymentConfig;
import ai.datalithix.kanon.modelregistry.model.DeploymentTarget;
import ai.datalithix.kanon.modelregistry.model.EvaluationMetric;
import ai.datalithix.kanon.modelregistry.model.EvaluationRun;
import ai.datalithix.kanon.modelregistry.model.ModelArtifact;
import ai.datalithix.kanon.modelregistry.model.ModelEntry;
import ai.datalithix.kanon.modelregistry.model.ModelLifecycleStage;
import ai.datalithix.kanon.modelregistry.model.ModelVersion;
import ai.datalithix.kanon.modelregistry.service.ModelRegistryRepository;
import ai.datalithix.kanon.training.model.HyperParameterConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
public class PostgresModelRegistryRepository implements ModelRegistryRepository {
    private final JdbcTemplate jdbcTemplate;

    public PostgresModelRegistryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ModelEntry saveEntry(ModelEntry entry) {
        jdbcTemplate.update("""
                INSERT INTO model_entry (
                    model_entry_id, tenant_id, model_name, description, framework,
                    task_type, domain_type, latest_version_number, latest_lifecycle_stage,
                    latest_version_id, enabled, compliance_tags, version_ids,
                    created_at, created_by, updated_at, updated_by, audit_version
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, model_entry_id)
                DO UPDATE SET
                    model_name = EXCLUDED.model_name,
                    description = EXCLUDED.description,
                    framework = EXCLUDED.framework,
                    task_type = EXCLUDED.task_type,
                    domain_type = EXCLUDED.domain_type,
                    latest_version_number = EXCLUDED.latest_version_number,
                    latest_lifecycle_stage = EXCLUDED.latest_lifecycle_stage,
                    latest_version_id = EXCLUDED.latest_version_id,
                    enabled = EXCLUDED.enabled,
                    compliance_tags = EXCLUDED.compliance_tags,
                    version_ids = EXCLUDED.version_ids,
                    updated_at = EXCLUDED.updated_at,
                    updated_by = EXCLUDED.updated_by,
                    audit_version = model_entry.audit_version + 1
                """,
                entry.modelEntryId(), entry.tenantId(), entry.modelName(), entry.description(),
                entry.framework(), entry.taskType(), entry.domainType(),
                entry.latestVersionNumber(), entry.latestLifecycleStage(),
                entry.latestVersionId(), entry.enabled(),
                toJson(entry.complianceTags()), toJson(entry.versionIds()),
                timestamp(entry.audit().createdAt()), entry.audit().createdBy(),
                timestamp(entry.audit().updatedAt()), entry.audit().updatedBy(),
                entry.audit().version()
        );
        return findEntryById(entry.tenantId(), entry.modelEntryId()).orElse(entry);
    }

    @Override
    public Optional<ModelEntry> findEntryById(String tenantId, String modelEntryId) {
        return jdbcTemplate.query("SELECT * FROM model_entry WHERE tenant_id = ? AND model_entry_id = ?",
                this::mapEntry, tenantId, modelEntryId).stream().findFirst();
    }

    @Override
    public List<ModelEntry> findEntriesByTenant(String tenantId) {
        return jdbcTemplate.query("SELECT * FROM model_entry WHERE tenant_id = ? ORDER BY updated_at DESC, model_name ASC",
                this::mapEntry, tenantId);
    }

    @Override
    public List<ModelEntry> findEntriesByName(String tenantId, String modelName) {
        return jdbcTemplate.query("SELECT * FROM model_entry WHERE tenant_id = ? AND model_name = ? ORDER BY updated_at DESC",
                this::mapEntry, tenantId, modelName);
    }

    @Override
    public void deleteEntryById(String tenantId, String modelEntryId) {
        jdbcTemplate.update("DELETE FROM model_entry WHERE tenant_id = ? AND model_entry_id = ?",
                tenantId, modelEntryId);
    }

    @Override
    public ModelVersion saveVersion(ModelVersion version) {
        jdbcTemplate.update("""
                INSERT INTO model_version (
                    model_version_id, model_entry_id, tenant_id, version_number,
                    training_job_id, dataset_version_id, dataset_definition_id,
                    artifact_uri, artifact_size, artifact_type, artifact_storage_backend,
                    lifecycle_stage, promoted_by, promoted_at,
                    artifact_json, hyper_parameters_json, compliance_tags,
                    evaluation_run_ids, deployment_target_ids, evidence_event_ids,
                    created_at, created_by, updated_at, updated_by, audit_version
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, model_version_id)
                DO UPDATE SET
                    training_job_id = EXCLUDED.training_job_id,
                    dataset_version_id = EXCLUDED.dataset_version_id,
                    dataset_definition_id = EXCLUDED.dataset_definition_id,
                    artifact_uri = EXCLUDED.artifact_uri,
                    artifact_size = EXCLUDED.artifact_size,
                    artifact_type = EXCLUDED.artifact_type,
                    artifact_storage_backend = EXCLUDED.artifact_storage_backend,
                    lifecycle_stage = EXCLUDED.lifecycle_stage,
                    promoted_by = EXCLUDED.promoted_by,
                    promoted_at = EXCLUDED.promoted_at,
                    artifact_json = EXCLUDED.artifact_json,
                    hyper_parameters_json = EXCLUDED.hyper_parameters_json,
                    compliance_tags = EXCLUDED.compliance_tags,
                    evaluation_run_ids = EXCLUDED.evaluation_run_ids,
                    deployment_target_ids = EXCLUDED.deployment_target_ids,
                    evidence_event_ids = EXCLUDED.evidence_event_ids,
                    updated_at = EXCLUDED.updated_at,
                    updated_by = EXCLUDED.updated_by,
                    audit_version = model_version.audit_version + 1
                """,
                version.modelVersionId(), version.modelEntryId(), version.tenantId(),
                version.versionNumber(), version.trainingJobId(), version.datasetVersionId(),
                version.datasetDefinitionId(),
                version.artifact().artifactUri(), version.artifact().sizeBytes(),
                version.artifact().framework(), version.artifact().storageType(),
                version.lifecycleStage().name(), version.promotedBy(),
                timestamp(version.promotedAt()),
                toJson(version.artifact()), toJson(version.hyperParameters()),
                toJson(version.complianceTags()), toJson(version.evaluationRunIds()),
                toJson(version.deploymentTargetIds()), toJson(version.evidenceEventIds()),
                timestamp(version.audit().createdAt()), version.audit().createdBy(),
                timestamp(version.audit().updatedAt()), version.audit().updatedBy(),
                version.audit().version()
        );
        return findVersionById(version.tenantId(), version.modelVersionId()).orElse(version);
    }

    @Override
    public Optional<ModelVersion> findVersionById(String tenantId, String modelVersionId) {
        return jdbcTemplate.query("SELECT * FROM model_version WHERE tenant_id = ? AND model_version_id = ?",
                this::mapVersion, tenantId, modelVersionId).stream().findFirst();
    }

    @Override
    public List<ModelVersion> findVersionsByEntryId(String tenantId, String modelEntryId) {
        return jdbcTemplate.query("""
                SELECT * FROM model_version
                WHERE tenant_id = ? AND model_entry_id = ?
                ORDER BY version_number DESC
                """, this::mapVersion, tenantId, modelEntryId);
    }

    @Override
    public Optional<ModelVersion> findLatestVersion(String tenantId, String modelEntryId) {
        return jdbcTemplate.query("""
                SELECT * FROM model_version
                WHERE tenant_id = ? AND model_entry_id = ?
                ORDER BY version_number DESC LIMIT 1
                """, this::mapVersion, tenantId, modelEntryId).stream().findFirst();
    }

    @Override
    public List<ModelVersion> findVersionsByStage(String tenantId, ModelLifecycleStage stage) {
        return jdbcTemplate.query("SELECT * FROM model_version WHERE tenant_id = ? AND lifecycle_stage = ? ORDER BY version_number DESC",
                this::mapVersion, tenantId, stage.name());
    }

    @Override
    public EvaluationRun saveEvaluation(EvaluationRun run) {
        jdbcTemplate.update("""
                INSERT INTO evaluation_run (
                    evaluation_run_id, model_version_id, model_entry_id, tenant_id,
                    test_dataset_version_id, status, failure_reason, started_at, completed_at,
                    passed_threshold, metrics_json, per_class_metrics_json,
                    confusion_matrix_uri, failure_case_sample_uri, evidence_event_ids,
                    created_at, created_by, updated_at, updated_by, audit_version
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, evaluation_run_id)
                DO UPDATE SET
                    model_version_id = EXCLUDED.model_version_id,
                    test_dataset_version_id = EXCLUDED.test_dataset_version_id,
                    status = EXCLUDED.status,
                    failure_reason = EXCLUDED.failure_reason,
                    started_at = EXCLUDED.started_at,
                    completed_at = EXCLUDED.completed_at,
                    passed_threshold = EXCLUDED.passed_threshold,
                    metrics_json = EXCLUDED.metrics_json,
                    per_class_metrics_json = EXCLUDED.per_class_metrics_json,
                    confusion_matrix_uri = EXCLUDED.confusion_matrix_uri,
                    failure_case_sample_uri = EXCLUDED.failure_case_sample_uri,
                    evidence_event_ids = EXCLUDED.evidence_event_ids,
                    updated_at = EXCLUDED.updated_at,
                    updated_by = EXCLUDED.updated_by,
                    audit_version = evaluation_run.audit_version + 1
                """,
                run.evaluationRunId(), run.modelVersionId(), run.modelEntryId(), run.tenantId(),
                run.testDatasetVersionId(), run.status(), run.failureReason(),
                timestamp(run.startedAt()), timestamp(run.completedAt()),
                run.passedThreshold(),
                toJson(run.metrics()), toJson(run.perClassMetrics()),
                run.confusionMatrixUri(), run.failureCaseSampleUri(),
                toJson(run.evidenceEventIds()),
                timestamp(run.audit().createdAt()), run.audit().createdBy(),
                timestamp(run.audit().updatedAt()), run.audit().updatedBy(),
                run.audit().version()
        );
        return findEvaluationById(run.tenantId(), run.evaluationRunId()).orElse(run);
    }

    @Override
    public Optional<EvaluationRun> findEvaluationById(String tenantId, String evaluationRunId) {
        return jdbcTemplate.query("SELECT * FROM evaluation_run WHERE tenant_id = ? AND evaluation_run_id = ?",
                this::mapEvaluation, tenantId, evaluationRunId).stream().findFirst();
    }

    @Override
    public List<EvaluationRun> findEvaluationsByVersion(String tenantId, String modelVersionId) {
        return jdbcTemplate.query("SELECT * FROM evaluation_run WHERE tenant_id = ? AND model_version_id = ? ORDER BY started_at DESC",
                this::mapEvaluation, tenantId, modelVersionId);
    }

    @Override
    public DeploymentTarget saveDeployment(DeploymentTarget target) {
        jdbcTemplate.update("""
                INSERT INTO deployment_target (
                    deployment_target_id, model_version_id, model_entry_id, tenant_id,
                    target_type, endpoint_url, health_status, healthy, last_health_check_at,
                    credential_ref, active, deployed_at, rolled_back_at,
                    config_json, evidence_event_ids,
                    created_at, created_by, updated_at, updated_by, audit_version
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, deployment_target_id)
                DO UPDATE SET
                    model_version_id = EXCLUDED.model_version_id,
                    target_type = EXCLUDED.target_type,
                    endpoint_url = EXCLUDED.endpoint_url,
                    health_status = EXCLUDED.health_status,
                    healthy = EXCLUDED.healthy,
                    last_health_check_at = EXCLUDED.last_health_check_at,
                    credential_ref = EXCLUDED.credential_ref,
                    active = EXCLUDED.active,
                    deployed_at = EXCLUDED.deployed_at,
                    rolled_back_at = EXCLUDED.rolled_back_at,
                    config_json = EXCLUDED.config_json,
                    evidence_event_ids = EXCLUDED.evidence_event_ids,
                    updated_at = EXCLUDED.updated_at,
                    updated_by = EXCLUDED.updated_by,
                    audit_version = deployment_target.audit_version + 1
                """,
                target.deploymentTargetId(), target.modelVersionId(), target.modelEntryId(),
                target.tenantId(), target.targetType(), target.endpointUrl(),
                target.healthStatus(), target.healthy(), timestamp(target.lastHealthCheckAt()),
                target.credentialRef(), target.active(), timestamp(target.deployedAt()),
                timestamp(target.rolledBackAt()),
                toJson(target.config()), toJson(target.evidenceEventIds()),
                timestamp(target.audit().createdAt()), target.audit().createdBy(),
                timestamp(target.audit().updatedAt()), target.audit().updatedBy(),
                target.audit().version()
        );
        return findDeploymentById(target.tenantId(), target.deploymentTargetId()).orElse(target);
    }

    @Override
    public Optional<DeploymentTarget> findDeploymentById(String tenantId, String deploymentTargetId) {
        return jdbcTemplate.query("SELECT * FROM deployment_target WHERE tenant_id = ? AND deployment_target_id = ?",
                this::mapDeployment, tenantId, deploymentTargetId).stream().findFirst();
    }

    @Override
    public List<DeploymentTarget> findDeploymentsByVersion(String tenantId, String modelVersionId) {
        return jdbcTemplate.query("SELECT * FROM deployment_target WHERE tenant_id = ? AND model_version_id = ? ORDER BY deployed_at DESC",
                this::mapDeployment, tenantId, modelVersionId);
    }

    @Override
    public List<DeploymentTarget> findActiveDeployments(String tenantId) {
        return jdbcTemplate.query("SELECT * FROM deployment_target WHERE tenant_id = ? AND active = TRUE ORDER BY deployed_at DESC",
                this::mapDeployment, tenantId);
    }

    @Override
    public PageResult<ModelEntry> findPage(QuerySpec query) {
        int limit = query.page().pageSize();
        int offset = query.page().pageNumber() * query.page().pageSize();
        List<ModelEntry> items = jdbcTemplate.query("""
                SELECT * FROM model_entry
                WHERE tenant_id = ?
                ORDER BY updated_at DESC, model_name ASC
                LIMIT ? OFFSET ?
                """, this::mapEntry, query.tenantId(), limit, offset);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM model_entry WHERE tenant_id = ?", Long.class, query.tenantId());
        return new PageResult<>(items, query.page().pageNumber(), query.page().pageSize(), total == null ? 0 : total);
    }

    private ModelEntry mapEntry(ResultSet rs, int rowNum) throws SQLException {
        return new ModelEntry(
                rs.getString("model_entry_id"),
                rs.getString("tenant_id"),
                rs.getString("model_name"),
                rs.getString("description"),
                rs.getString("framework"),
                rs.getString("task_type"),
                rs.getString("domain_type"),
                fromJson(rs.getString("compliance_tags"), new TypeReference<Set<String>>() {}),
                rs.getInt("latest_version_number"),
                rs.getString("latest_lifecycle_stage"),
                rs.getString("latest_version_id"),
                rs.getBoolean("enabled"),
                fromJson(rs.getString("version_ids"), new TypeReference<List<String>>() {}),
                audit(rs)
        );
    }

    private ModelVersion mapVersion(ResultSet rs, int rowNum) throws SQLException {
        ModelArtifact flatArtifact = new ModelArtifact(
                rs.getString("artifact_uri"),
                rs.getString("artifact_type"),
                rs.getLong("artifact_size"),
                null,
                rs.getString("artifact_storage_backend")
        );
        ModelArtifact jsonArtifact = fromJson(rs.getString("artifact_json"), ModelArtifact.class);
        ModelArtifact artifact = jsonArtifact != null ? jsonArtifact : flatArtifact;
        return new ModelVersion(
                rs.getString("model_version_id"),
                rs.getString("model_entry_id"),
                rs.getString("tenant_id"),
                rs.getInt("version_number"),
                rs.getString("training_job_id"),
                rs.getString("dataset_version_id"),
                rs.getString("dataset_definition_id"),
                artifact,
                fromJson(rs.getString("hyper_parameters_json"), HyperParameterConfig.class),
                fromJson(rs.getString("compliance_tags"), new TypeReference<Set<String>>() {}),
                ModelLifecycleStage.valueOf(rs.getString("lifecycle_stage")),
                rs.getString("promoted_by"),
                instant(rs, "promoted_at"),
                fromJson(rs.getString("evaluation_run_ids"), new TypeReference<List<String>>() {}),
                fromJson(rs.getString("deployment_target_ids"), new TypeReference<List<String>>() {}),
                fromJson(rs.getString("evidence_event_ids"), new TypeReference<List<String>>() {}),
                audit(rs)
        );
    }

    private EvaluationRun mapEvaluation(ResultSet rs, int rowNum) throws SQLException {
        return new EvaluationRun(
                rs.getString("evaluation_run_id"),
                rs.getString("model_version_id"),
                rs.getString("model_entry_id"),
                rs.getString("tenant_id"),
                rs.getString("test_dataset_version_id"),
                fromJson(rs.getString("metrics_json"), new TypeReference<List<EvaluationMetric>>() {}),
                fromJson(rs.getString("per_class_metrics_json"), new TypeReference<Map<String, Double>>() {}),
                rs.getString("confusion_matrix_uri"),
                rs.getString("failure_case_sample_uri"),
                rs.getString("status"),
                rs.getString("failure_reason"),
                instant(rs, "started_at"),
                instant(rs, "completed_at"),
                rs.getBoolean("passed_threshold"),
                fromJson(rs.getString("evidence_event_ids"), new TypeReference<List<String>>() {}),
                audit(rs)
        );
    }

    private DeploymentTarget mapDeployment(ResultSet rs, int rowNum) throws SQLException {
        return new DeploymentTarget(
                rs.getString("deployment_target_id"),
                rs.getString("model_version_id"),
                rs.getString("model_entry_id"),
                rs.getString("tenant_id"),
                rs.getString("target_type"),
                rs.getString("endpoint_url"),
                rs.getString("health_status"),
                rs.getBoolean("healthy"),
                instant(rs, "last_health_check_at"),
                rs.getString("credential_ref"),
                fromJson(rs.getString("config_json"), DeploymentConfig.class),
                rs.getBoolean("active"),
                instant(rs, "deployed_at"),
                instant(rs, "rolled_back_at"),
                fromJson(rs.getString("evidence_event_ids"), new TypeReference<List<String>>() {}),
                audit(rs)
        );
    }
}

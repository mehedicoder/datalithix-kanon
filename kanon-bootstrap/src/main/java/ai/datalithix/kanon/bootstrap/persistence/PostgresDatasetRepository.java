package ai.datalithix.kanon.bootstrap.persistence;

import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.dataset.model.CurationRule;
import ai.datalithix.kanon.dataset.model.DatasetDefinition;
import ai.datalithix.kanon.dataset.model.DatasetSplit;
import ai.datalithix.kanon.dataset.model.DatasetVersion;
import ai.datalithix.kanon.dataset.model.ExportFormat;
import ai.datalithix.kanon.dataset.model.SplitStrategy;
import ai.datalithix.kanon.dataset.service.DatasetRepository;
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
public class PostgresDatasetRepository implements DatasetRepository {
    private final JdbcTemplate jdbcTemplate;

    public PostgresDatasetRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DatasetDefinition save(DatasetDefinition definition) {
        jdbcTemplate.update("""
                INSERT INTO dataset_definition (
                    dataset_definition_id, tenant_id, name, description, domain_type,
                    split_strategy, train_ratio, val_ratio, test_ratio, data_residency,
                    enabled, latest_version_number, source_annotation_ids,
                    curation_rule_json, export_formats,
                    created_at, created_by, updated_at, updated_by, audit_version
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, dataset_definition_id)
                DO UPDATE SET
                    name = EXCLUDED.name,
                    description = EXCLUDED.description,
                    domain_type = EXCLUDED.domain_type,
                    split_strategy = EXCLUDED.split_strategy,
                    train_ratio = EXCLUDED.train_ratio,
                    val_ratio = EXCLUDED.val_ratio,
                    test_ratio = EXCLUDED.test_ratio,
                    data_residency = EXCLUDED.data_residency,
                    enabled = EXCLUDED.enabled,
                    latest_version_number = EXCLUDED.latest_version_number,
                    source_annotation_ids = EXCLUDED.source_annotation_ids,
                    curation_rule_json = EXCLUDED.curation_rule_json,
                    export_formats = EXCLUDED.export_formats,
                    updated_at = EXCLUDED.updated_at,
                    updated_by = EXCLUDED.updated_by,
                    audit_version = dataset_definition.audit_version + 1
                """,
                definition.datasetDefinitionId(), definition.tenantId(), definition.name(),
                definition.description(), definition.domainType(),
                definition.splitStrategy().name(), definition.trainRatio(), definition.valRatio(),
                definition.testRatio(), definition.dataResidency(), definition.enabled(),
                definition.latestVersionNumber(),
                toJson(definition.sourceAnnotationIds()),
                toJson(definition.curationRule()),
                toJson(definition.exportFormats()),
                timestamp(definition.audit().createdAt()), definition.audit().createdBy(),
                timestamp(definition.audit().updatedAt()), definition.audit().updatedBy(),
                definition.audit().version()
        );
        return findById(definition.tenantId(), definition.datasetDefinitionId()).orElse(definition);
    }

    @Override
    public Optional<DatasetDefinition> findById(String tenantId, String datasetDefinitionId) {
        return jdbcTemplate.query("SELECT * FROM dataset_definition WHERE tenant_id = ? AND dataset_definition_id = ?",
                this::mapDefinition, tenantId, datasetDefinitionId).stream().findFirst();
    }

    @Override
    public List<DatasetDefinition> findByName(String tenantId, String name) {
        return jdbcTemplate.query("SELECT * FROM dataset_definition WHERE tenant_id = ? AND name = ? ORDER BY updated_at DESC",
                this::mapDefinition, tenantId, name);
    }

    @Override
    public List<DatasetDefinition> findByTenant(String tenantId) {
        return jdbcTemplate.query("SELECT * FROM dataset_definition WHERE tenant_id = ? ORDER BY updated_at DESC, name ASC",
                this::mapDefinition, tenantId);
    }

    @Override
    public void deleteById(String tenantId, String datasetDefinitionId) {
        jdbcTemplate.update("DELETE FROM dataset_definition WHERE tenant_id = ? AND dataset_definition_id = ?",
                tenantId, datasetDefinitionId);
    }

    @Override
    public DatasetVersion saveVersion(DatasetVersion version) {
        jdbcTemplate.update("""
                INSERT INTO dataset_version (
                    dataset_version_id, dataset_definition_id, tenant_id, version_number,
                    curation_rule_id, split_strategy, total_record_count, curated_at, curated_by,
                    export_status, export_format, export_artifact_uri, failure_reason,
                    splits_json, label_distribution_json, class_balance_json, evidence_event_ids,
                    created_at, created_by, updated_at, updated_by, audit_version
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, dataset_version_id)
                DO UPDATE SET
                    export_status = EXCLUDED.export_status,
                    export_format = EXCLUDED.export_format,
                    export_artifact_uri = EXCLUDED.export_artifact_uri,
                    failure_reason = EXCLUDED.failure_reason,
                    splits_json = EXCLUDED.splits_json,
                    label_distribution_json = EXCLUDED.label_distribution_json,
                    class_balance_json = EXCLUDED.class_balance_json,
                    evidence_event_ids = EXCLUDED.evidence_event_ids,
                    updated_at = EXCLUDED.updated_at,
                    updated_by = EXCLUDED.updated_by,
                    audit_version = dataset_version.audit_version + 1
                """,
                version.datasetVersionId(), version.datasetDefinitionId(), version.tenantId(),
                version.versionNumber(), version.curationRuleId(),
                version.splitStrategy() == null ? null : version.splitStrategy().name(),
                version.totalRecordCount(), timestamp(version.curatedAt()), version.curatedBy(),
                version.exportStatus(), version.exportFormat(), version.exportArtifactUri(),
                version.failureReason(),
                toJson(version.splits()), toJson(version.labelDistribution()),
                toJson(version.classBalance()), toJson(version.evidenceEventIds()),
                timestamp(version.audit().createdAt()), version.audit().createdBy(),
                timestamp(version.audit().updatedAt()), version.audit().updatedBy(),
                version.audit().version()
        );
        return findVersionById(version.tenantId(), version.datasetVersionId()).orElse(version);
    }

    @Override
    public Optional<DatasetVersion> findVersionById(String tenantId, String datasetVersionId) {
        return jdbcTemplate.query("SELECT * FROM dataset_version WHERE tenant_id = ? AND dataset_version_id = ?",
                this::mapVersion, tenantId, datasetVersionId).stream().findFirst();
    }

    @Override
    public List<DatasetVersion> findVersionsByDefinitionId(String tenantId, String datasetDefinitionId) {
        return jdbcTemplate.query("""
                SELECT * FROM dataset_version
                WHERE tenant_id = ? AND dataset_definition_id = ?
                ORDER BY version_number DESC
                """, this::mapVersion, tenantId, datasetDefinitionId);
    }

    @Override
    public Optional<DatasetVersion> findLatestVersion(String tenantId, String datasetDefinitionId) {
        return jdbcTemplate.query("""
                SELECT * FROM dataset_version
                WHERE tenant_id = ? AND dataset_definition_id = ?
                ORDER BY version_number DESC LIMIT 1
                """, this::mapVersion, tenantId, datasetDefinitionId).stream().findFirst();
    }

    @Override
    public PageResult<DatasetDefinition> findPage(QuerySpec query) {
        int limit = query.page().pageSize();
        int offset = query.page().pageNumber() * query.page().pageSize();
        List<DatasetDefinition> items = jdbcTemplate.query("""
                SELECT * FROM dataset_definition
                WHERE tenant_id = ?
                ORDER BY updated_at DESC, name ASC
                LIMIT ? OFFSET ?
                """, this::mapDefinition, query.tenantId(), limit, offset);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dataset_definition WHERE tenant_id = ?", Long.class, query.tenantId());
        return new PageResult<>(items, query.page().pageNumber(), query.page().pageSize(), total == null ? 0 : total);
    }

    private DatasetDefinition mapDefinition(ResultSet rs, int rowNum) throws SQLException {
        return new DatasetDefinition(
                rs.getString("dataset_definition_id"),
                rs.getString("tenant_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("domain_type"),
                fromJson(rs.getString("source_annotation_ids"), new TypeReference<Set<String>>() {}),
                fromJson(rs.getString("curation_rule_json"), CurationRule.class),
                SplitStrategy.valueOf(rs.getString("split_strategy")),
                rs.getDouble("train_ratio"),
                rs.getDouble("val_ratio"),
                rs.getDouble("test_ratio"),
                fromJson(rs.getString("export_formats"), new TypeReference<List<ExportFormat>>() {}),
                rs.getString("data_residency"),
                rs.getBoolean("enabled"),
                rs.getInt("latest_version_number"),
                audit(rs)
        );
    }

    private DatasetVersion mapVersion(ResultSet rs, int rowNum) throws SQLException {
        return new DatasetVersion(
                rs.getString("dataset_version_id"),
                rs.getString("dataset_definition_id"),
                rs.getString("tenant_id"),
                rs.getInt("version_number"),
                rs.getString("curation_rule_id"),
                fromJson(rs.getString("split_strategy"), SplitStrategy.class),
                fromJson(rs.getString("splits_json"), new TypeReference<List<DatasetSplit>>() {}),
                fromJson(rs.getString("label_distribution_json"), new TypeReference<Map<String, Long>>() {}),
                fromJson(rs.getString("class_balance_json"), new TypeReference<Map<String, Double>>() {}),
                rs.getLong("total_record_count"),
                instant(rs, "curated_at"),
                rs.getString("curated_by"),
                rs.getString("export_status"),
                rs.getString("export_format"),
                rs.getString("export_artifact_uri"),
                rs.getString("failure_reason"),
                fromJson(rs.getString("evidence_event_ids"), new TypeReference<List<String>>() {}),
                audit(rs)
        );
    }
}

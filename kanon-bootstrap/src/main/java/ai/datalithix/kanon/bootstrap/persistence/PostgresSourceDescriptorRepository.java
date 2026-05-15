package ai.datalithix.kanon.bootstrap.persistence;

import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DataResidency;
import ai.datalithix.kanon.common.SourceCategory;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.common.compliance.ComplianceClassification;
import ai.datalithix.kanon.common.compliance.DataClassification;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.ingestion.model.SourceDescriptor;
import ai.datalithix.kanon.ingestion.service.SourceDescriptorRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.timestamp;

@Repository
@Profile("!test")
public class PostgresSourceDescriptorRepository implements SourceDescriptorRepository {
    private final JdbcTemplate jdbcTemplate;

    public PostgresSourceDescriptorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public SourceDescriptor save(String tenantId, SourceDescriptor descriptor) {
        jdbcTemplate.update("""
                INSERT INTO source_descriptors (
                    source_system, source_identifier, tenant_id, source_category, source_type,
                    source_uri, asset_type, data_classification, compliance_classification,
                    data_residency, retention_policy, consent_ref,
                    created_at, created_by, updated_at, updated_by, audit_version
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, source_system, source_identifier)
                DO UPDATE SET
                    source_category = EXCLUDED.source_category,
                    source_type = EXCLUDED.source_type,
                    source_uri = EXCLUDED.source_uri,
                    asset_type = EXCLUDED.asset_type,
                    data_classification = EXCLUDED.data_classification,
                    compliance_classification = EXCLUDED.compliance_classification,
                    data_residency = EXCLUDED.data_residency,
                    retention_policy = EXCLUDED.retention_policy,
                    consent_ref = EXCLUDED.consent_ref,
                    updated_at = EXCLUDED.updated_at,
                    updated_by = EXCLUDED.updated_by,
                    audit_version = source_descriptors.audit_version + 1
                """,
                descriptor.sourceSystem(), descriptor.sourceIdentifier(), tenantId,
                descriptor.sourceCategory().name(), descriptor.sourceType().name(),
                descriptor.sourceUri(),
                descriptor.assetType() != null ? descriptor.assetType().name() : null,
                descriptor.dataClassification() != null ? descriptor.dataClassification().name() : null,
                descriptor.complianceClassification() != null ? descriptor.complianceClassification().name() : null,
                descriptor.dataResidency() != null ? descriptor.dataResidency().name() : null,
                descriptor.retentionPolicy(), descriptor.consentRef(),
                timestamp(java.time.Instant.now()), "system",
                timestamp(java.time.Instant.now()), "system", 1L
        );
        return findBySourceIdentity(tenantId, descriptor.sourceSystem(), descriptor.sourceIdentifier()).orElse(descriptor);
    }

    @Override
    public Optional<SourceDescriptor> findBySourceIdentity(String tenantId, String sourceSystem, String sourceIdentifier) {
        return jdbcTemplate.query("""
                        SELECT * FROM source_descriptors
                        WHERE tenant_id = ? AND source_system = ? AND source_identifier = ?
                        """,
                this::map, tenantId, sourceSystem, sourceIdentifier).stream().findFirst();
    }

    @Override
    public PageResult<SourceDescriptor> findPage(QuerySpec query) {
        int limit = query.page().pageSize();
        int offset = query.page().pageNumber() * limit;
        var items = jdbcTemplate.query("""
                        SELECT * FROM source_descriptors
                        WHERE tenant_id = ?
                        ORDER BY updated_at DESC
                        LIMIT ? OFFSET ?
                        """,
                this::map, query.tenantId(), limit, offset);
        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM source_descriptors WHERE tenant_id = ?",
                Long.class, query.tenantId());
        return new PageResult<>(items, query.page().pageNumber(), query.page().pageSize(), total);
    }

    private SourceDescriptor map(ResultSet rs, int rowNum) throws SQLException {
        return new SourceDescriptor(
                SourceCategory.valueOf(rs.getString("source_category")),
                SourceType.valueOf(rs.getString("source_type")),
                rs.getString("source_system"),
                rs.getString("source_identifier"),
                rs.getString("source_uri"),
                enumOrNull(rs.getString("asset_type"), AssetType.class),
                enumOrNull(rs.getString("data_classification"), DataClassification.class),
                enumOrNull(rs.getString("compliance_classification"), ComplianceClassification.class),
                enumOrNull(rs.getString("data_residency"), DataResidency.class),
                rs.getString("retention_policy"),
                rs.getString("consent_ref")
        );
    }

    private static <E extends Enum<E>> E enumOrNull(String value, Class<E> type) {
        return value == null || value.isBlank() ? null : Enum.valueOf(type, value);
    }
}

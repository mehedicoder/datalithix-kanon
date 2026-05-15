package ai.datalithix.kanon.bootstrap.persistence;

import ai.datalithix.kanon.common.ActorType;
import ai.datalithix.kanon.common.SourceCategory;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.ingestion.model.SourceDescriptor;
import ai.datalithix.kanon.ingestion.model.SourceTrace;
import ai.datalithix.kanon.ingestion.model.SourceTraceDetails;
import ai.datalithix.kanon.ingestion.service.SourceTraceRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.audit;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.fromJson;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.instant;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.optionalEnum;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.timestamp;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.toJson;

@Repository
@Profile("!test")
public class PostgresSourceTraceRepository implements SourceTraceRepository {
    private final JdbcTemplate jdbcTemplate;

    public PostgresSourceTraceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public SourceTrace save(SourceTrace trace) {
        jdbcTemplate.update("""
                INSERT INTO source_traces (
                    source_trace_id, tenant_id, case_id,
                    source_system, source_identifier, source_category, source_type,
                    source_uri, asset_type, data_classification, compliance_classification,
                    data_residency, retention_policy, consent_ref,
                    original_payload_hash, actor_type, actor_id,
                    ingestion_timestamp, correlation_id, evidence_event_id, details_json,
                    created_at, created_by, updated_at, updated_by, audit_version
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, source_trace_id)
                DO UPDATE SET
                    case_id = EXCLUDED.case_id,
                    source_uri = EXCLUDED.source_uri,
                    details_json = EXCLUDED.details_json,
                    updated_at = EXCLUDED.updated_at,
                    updated_by = EXCLUDED.updated_by,
                    audit_version = source_traces.audit_version + 1
                """,
                trace.sourceTraceId(), trace.tenantId(), trace.caseId(),
                trace.source().sourceSystem(), trace.source().sourceIdentifier(),
                trace.source().sourceCategory().name(), trace.source().sourceType().name(),
                trace.source().sourceUri(),
                trace.source().assetType() != null ? trace.source().assetType().name() : null,
                trace.source().dataClassification() != null ? trace.source().dataClassification().name() : null,
                trace.source().complianceClassification() != null ? trace.source().complianceClassification().name() : null,
                trace.source().dataResidency() != null ? trace.source().dataResidency().name() : null,
                trace.source().retentionPolicy(), trace.source().consentRef(),
                trace.originalPayloadHash(), trace.actorType().name(), trace.actorId(),
                timestamp(trace.ingestionTimestamp()), trace.correlationId(), trace.evidenceEventId(),
                toJson(trace.details()),
                timestamp(trace.audit().createdAt()), trace.audit().createdBy(),
                timestamp(trace.audit().updatedAt()), trace.audit().updatedBy(),
                trace.audit().version()
        );
        return findById(trace.tenantId(), trace.sourceTraceId()).orElse(trace);
    }

    @Override
    public Optional<SourceTrace> findById(String tenantId, String sourceTraceId) {
        return jdbcTemplate.query("SELECT * FROM source_traces WHERE tenant_id = ? AND source_trace_id = ?",
                this::map, tenantId, sourceTraceId).stream().findFirst();
    }

    @Override
    public Optional<SourceTrace> findByCorrelationId(String tenantId, String correlationId) {
        return jdbcTemplate.query("SELECT * FROM source_traces WHERE tenant_id = ? AND correlation_id = ?",
                this::map, tenantId, correlationId).stream().findFirst();
    }

    @Override
    public Optional<SourceTrace> findBySourceIdentity(String tenantId, String sourceSystem, String sourceIdentifier) {
        return jdbcTemplate.query("""
                        SELECT * FROM source_traces
                        WHERE tenant_id = ? AND source_system = ? AND source_identifier = ?
                        ORDER BY ingestion_timestamp DESC LIMIT 1
                        """,
                this::map, tenantId, sourceSystem, sourceIdentifier).stream().findFirst();
    }

    @Override
    public List<SourceTrace> findByCaseId(String tenantId, String caseId) {
        return jdbcTemplate.query("""
                        SELECT * FROM source_traces
                        WHERE tenant_id = ? AND case_id = ?
                        ORDER BY ingestion_timestamp DESC
                        """,
                this::map, tenantId, caseId);
    }

    @Override
    public PageResult<SourceTrace> findPage(QuerySpec query) {
        int limit = query.page().pageSize();
        int offset = query.page().pageNumber() * limit;
        var items = jdbcTemplate.query("""
                        SELECT * FROM source_traces
                        WHERE tenant_id = ?
                        ORDER BY ingestion_timestamp DESC
                        LIMIT ? OFFSET ?
                        """,
                this::map, query.tenantId(), limit, offset);
        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM source_traces WHERE tenant_id = ?",
                Long.class, query.tenantId());
        return new PageResult<>(items, query.page().pageNumber(), query.page().pageSize(), total);
    }

    private SourceTrace map(ResultSet rs, int rowNum) throws SQLException {
        var source = new SourceDescriptor(
                SourceCategory.valueOf(rs.getString("source_category")),
                SourceType.valueOf(rs.getString("source_type")),
                rs.getString("source_system"),
                rs.getString("source_identifier"),
                rs.getString("source_uri"),
                optionalEnum(rs.getString("asset_type"), ai.datalithix.kanon.common.AssetType::valueOf),
                optionalEnum(rs.getString("data_classification"), ai.datalithix.kanon.common.compliance.DataClassification::valueOf),
                optionalEnum(rs.getString("compliance_classification"), ai.datalithix.kanon.common.compliance.ComplianceClassification::valueOf),
                optionalEnum(rs.getString("data_residency"), ai.datalithix.kanon.common.DataResidency::valueOf),
                rs.getString("retention_policy"),
                rs.getString("consent_ref")
        );
        return new SourceTrace(
                rs.getString("source_trace_id"),
                rs.getString("tenant_id"),
                rs.getString("case_id"),
                source,
                rs.getString("original_payload_hash"),
                ActorType.valueOf(rs.getString("actor_type")),
                rs.getString("actor_id"),
                instant(rs, "ingestion_timestamp"),
                rs.getString("correlation_id"),
                rs.getString("evidence_event_id"),
                fromJson(rs.getString("details_json"), new TypeReference<SourceTraceDetails>() {}),
                audit(rs)
        );
    }
}

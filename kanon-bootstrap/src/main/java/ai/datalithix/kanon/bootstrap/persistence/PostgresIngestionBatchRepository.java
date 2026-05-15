package ai.datalithix.kanon.bootstrap.persistence;

import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.ingestion.model.IngestionBatch;
import ai.datalithix.kanon.ingestion.model.IngestionStatus;
import ai.datalithix.kanon.ingestion.service.IngestionBatchRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.audit;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.instant;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.timestamp;

@Repository
@Profile("!test")
public class PostgresIngestionBatchRepository implements IngestionBatchRepository {
    private final JdbcTemplate jdbcTemplate;

    public PostgresIngestionBatchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public IngestionBatch save(IngestionBatch batch) {
        jdbcTemplate.update("""
                INSERT INTO ingestion_batches (
                    import_batch_id, tenant_id, connector_id, source_system, status,
                    received_count, accepted_count, rejected_count, retry_count,
                    started_at, completed_at, failure_reason,
                    created_at, created_by, updated_at, updated_by, audit_version
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, import_batch_id)
                DO UPDATE SET
                    status = EXCLUDED.status,
                    received_count = EXCLUDED.received_count,
                    accepted_count = EXCLUDED.accepted_count,
                    rejected_count = EXCLUDED.rejected_count,
                    retry_count = EXCLUDED.retry_count,
                    completed_at = EXCLUDED.completed_at,
                    failure_reason = EXCLUDED.failure_reason,
                    updated_at = EXCLUDED.updated_at,
                    updated_by = EXCLUDED.updated_by,
                    audit_version = ingestion_batches.audit_version + 1
                """,
                batch.importBatchId(), batch.tenantId(), batch.connectorId(), batch.sourceSystem(),
                batch.status().name(), batch.receivedCount(), batch.acceptedCount(),
                batch.rejectedCount(), batch.retryCount(),
                timestamp(batch.startedAt()), timestamp(batch.completedAt()),
                batch.failureReason(),
                timestamp(batch.audit().createdAt()), batch.audit().createdBy(),
                timestamp(batch.audit().updatedAt()), batch.audit().updatedBy(),
                batch.audit().version()
        );
        return findById(batch.tenantId(), batch.importBatchId()).orElse(batch);
    }

    @Override
    public Optional<IngestionBatch> findById(String tenantId, String importBatchId) {
        return jdbcTemplate.query("SELECT * FROM ingestion_batches WHERE tenant_id = ? AND import_batch_id = ?",
                this::map, tenantId, importBatchId).stream().findFirst();
    }

    @Override
    public PageResult<IngestionBatch> findPage(QuerySpec query) {
        int limit = query.page().pageSize();
        int offset = query.page().pageNumber() * limit;
        var items = jdbcTemplate.query("""
                        SELECT * FROM ingestion_batches
                        WHERE tenant_id = ?
                        ORDER BY started_at DESC NULLS LAST
                        LIMIT ? OFFSET ?
                        """,
                this::map, query.tenantId(), limit, offset);
        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ingestion_batches WHERE tenant_id = ?",
                Long.class, query.tenantId());
        return new PageResult<>(items, query.page().pageNumber(), query.page().pageSize(), total);
    }

    private IngestionBatch map(ResultSet rs, int rowNum) throws SQLException {
        return new IngestionBatch(
                rs.getString("import_batch_id"),
                rs.getString("tenant_id"),
                rs.getString("connector_id"),
                rs.getString("source_system"),
                IngestionStatus.valueOf(rs.getString("status")),
                rs.getLong("received_count"),
                rs.getLong("accepted_count"),
                rs.getLong("rejected_count"),
                rs.getLong("retry_count"),
                instant(rs, "started_at"),
                instant(rs, "completed_at"),
                rs.getString("failure_reason"),
                audit(rs)
        );
    }
}

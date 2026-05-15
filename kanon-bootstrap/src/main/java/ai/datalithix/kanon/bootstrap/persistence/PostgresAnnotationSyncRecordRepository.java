package ai.datalithix.kanon.bootstrap.persistence;

import ai.datalithix.kanon.annotation.model.AnnotationExecutionNodeType;
import ai.datalithix.kanon.annotation.model.AnnotationSyncRecord;
import ai.datalithix.kanon.annotation.model.AnnotationTaskStatus;
import ai.datalithix.kanon.annotation.service.AnnotationSyncRecordRepository;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import com.fasterxml.jackson.core.type.TypeReference;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.*;

@Repository
@Profile("!test")
public class PostgresAnnotationSyncRecordRepository implements AnnotationSyncRecordRepository {
    private final JdbcTemplate jdbcTemplate;

    public PostgresAnnotationSyncRecordRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String save(String tenantId, AnnotationSyncRecord record) {
        jdbcTemplate.update("""
                INSERT INTO annotation_sync_records (
                    tenant_id, annotation_task_id, node_id, node_type,
                    external_task_id, status, external_url, failure_reason,
                    metadata_json, synced_at,
                    created_at, created_by, updated_at, updated_by, audit_version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?)
                """,
                tenantId,
                record.annotationTaskId(),
                record.nodeId(),
                record.nodeType().name(),
                record.externalTaskId(),
                record.status().name(),
                record.externalUrl(),
                record.failureReason(),
                toJson(record.metadata()),
                timestamp(record.syncedAt()),
                timestamp(java.time.Instant.now()), "system",
                timestamp(java.time.Instant.now()), "system", 1L
        );
        return jdbcTemplate.queryForObject("""
                SELECT sync_id::text FROM annotation_sync_records
                WHERE tenant_id = ? AND annotation_task_id = ? AND node_id = ?
                ORDER BY synced_at DESC LIMIT 1
                """, String.class, tenantId, record.annotationTaskId(), record.nodeId());
    }

    @Override
    public Optional<AnnotationSyncRecord> findById(String tenantId, String syncId) {
        return jdbcTemplate.query("""
                        SELECT * FROM annotation_sync_records
                        WHERE sync_id = ?::uuid AND tenant_id = ?
                        """,
                this::map, java.util.UUID.fromString(syncId), tenantId).stream().findFirst();
    }

    @Override
    public List<AnnotationSyncRecord> findByAnnotationTaskId(String tenantId, String annotationTaskId) {
        return jdbcTemplate.query("""
                        SELECT * FROM annotation_sync_records
                        WHERE tenant_id = ? AND annotation_task_id = ?
                        ORDER BY synced_at DESC
                        """,
                this::map, tenantId, annotationTaskId);
    }

    @Override
    public List<AnnotationSyncRecord> findByNodeId(String tenantId, String nodeId) {
        return jdbcTemplate.query("""
                        SELECT * FROM annotation_sync_records
                        WHERE tenant_id = ? AND node_id = ?
                        ORDER BY synced_at DESC
                        """,
                this::map, tenantId, nodeId);
    }

    @Override
    public List<AnnotationSyncRecord> findByStatus(String tenantId, AnnotationTaskStatus status) {
        return jdbcTemplate.query("""
                        SELECT * FROM annotation_sync_records
                        WHERE tenant_id = ? AND status = ?
                        ORDER BY synced_at DESC
                        """,
                this::map, tenantId, status.name());
    }

    @Override
    public PageResult<AnnotationSyncRecord> findPage(QuerySpec query) {
        int limit = query.page().pageSize();
        int offset = query.page().pageNumber() * limit;
        var items = jdbcTemplate.query("""
                        SELECT * FROM annotation_sync_records
                        WHERE tenant_id = ?
                        ORDER BY synced_at DESC
                        LIMIT ? OFFSET ?
                        """,
                this::map, query.tenantId(), limit, offset);
        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM annotation_sync_records WHERE tenant_id = ?",
                Long.class, query.tenantId());
        return new PageResult<>(items, query.page().pageNumber(), query.page().pageSize(), total);
    }

    private AnnotationSyncRecord map(ResultSet rs, int rowNum) throws SQLException {
        return new AnnotationSyncRecord(
                rs.getString("annotation_task_id"),
                rs.getString("node_id"),
                AnnotationExecutionNodeType.valueOf(rs.getString("node_type")),
                rs.getString("external_task_id"),
                AnnotationTaskStatus.valueOf(rs.getString("status")),
                rs.getString("external_url"),
                rs.getString("failure_reason"),
                fromJson(rs.getString("metadata_json"), new TypeReference<Map<String, String>>() {}),
                instant(rs, "synced_at")
        );
    }
}

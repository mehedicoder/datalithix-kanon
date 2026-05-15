package ai.datalithix.kanon.bootstrap.persistence;

import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.ingestion.model.ConnectorHealth;
import ai.datalithix.kanon.ingestion.model.ConnectorHealthStatus;
import ai.datalithix.kanon.ingestion.service.ConnectorHealthRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.instant;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.timestamp;

@Repository
@Profile("!test")
public class PostgresConnectorHealthRepository implements ConnectorHealthRepository {
    private final JdbcTemplate jdbcTemplate;

    public PostgresConnectorHealthRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ConnectorHealth save(String tenantId, ConnectorHealth health) {
        jdbcTemplate.update("""
                INSERT INTO connector_health (
                    connector_id, tenant_id, status,
                    last_ingestion_at, last_success_at, last_failure_at,
                    last_failure_reason, retry_count, lag_millis,
                    created_at, created_by, updated_at, updated_by, audit_version
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, connector_id)
                DO UPDATE SET
                    status = EXCLUDED.status,
                    last_ingestion_at = EXCLUDED.last_ingestion_at,
                    last_success_at = EXCLUDED.last_success_at,
                    last_failure_at = EXCLUDED.last_failure_at,
                    last_failure_reason = EXCLUDED.last_failure_reason,
                    retry_count = EXCLUDED.retry_count,
                    lag_millis = EXCLUDED.lag_millis,
                    updated_at = EXCLUDED.updated_at,
                    updated_by = EXCLUDED.updated_by,
                    audit_version = connector_health.audit_version + 1
                """,
                health.connectorId(), tenantId, health.status().name(),
                timestamp(health.lastIngestionAt()), timestamp(health.lastSuccessAt()),
                timestamp(health.lastFailureAt()), health.lastFailureReason(),
                health.retryCount(), health.lagMillis(),
                timestamp(java.time.Instant.now()), "system",
                timestamp(java.time.Instant.now()), "system", 1L
        );
        return findByConnectorId(tenantId, health.connectorId()).orElse(health);
    }

    @Override
    public Optional<ConnectorHealth> findByConnectorId(String tenantId, String connectorId) {
        return jdbcTemplate.query("SELECT * FROM connector_health WHERE tenant_id = ? AND connector_id = ?",
                this::map, tenantId, connectorId).stream().findFirst();
    }

    @Override
    public PageResult<ConnectorHealth> findPage(QuerySpec query) {
        int limit = query.page().pageSize();
        int offset = query.page().pageNumber() * limit;
        var items = jdbcTemplate.query("""
                        SELECT * FROM connector_health
                        WHERE tenant_id = ?
                        ORDER BY last_ingestion_at DESC NULLS LAST
                        LIMIT ? OFFSET ?
                        """,
                this::map, query.tenantId(), limit, offset);
        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM connector_health WHERE tenant_id = ?",
                Long.class, query.tenantId());
        return new PageResult<>(items, query.page().pageNumber(), query.page().pageSize(), total);
    }

    private ConnectorHealth map(ResultSet rs, int rowNum) throws SQLException {
        return new ConnectorHealth(
                rs.getString("connector_id"),
                ConnectorHealthStatus.valueOf(rs.getString("status")),
                instant(rs, "last_ingestion_at"),
                instant(rs, "last_success_at"),
                instant(rs, "last_failure_at"),
                rs.getString("last_failure_reason"),
                rs.getLong("retry_count"),
                rs.getLong("lag_millis")
        );
    }
}

package ai.datalithix.kanon.bootstrap.persistence;

import ai.datalithix.kanon.common.SourceCategory;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.ingestion.model.ConnectorConfiguration;
import ai.datalithix.kanon.ingestion.model.ConnectorExecutionPolicy;
import ai.datalithix.kanon.ingestion.service.ConnectorConfigurationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.audit;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.fromJson;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.optionalEnum;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.timestamp;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.toJson;

@Repository
@Profile("!test")
public class PostgresConnectorConfigurationRepository implements ConnectorConfigurationRepository {
    private final JdbcTemplate jdbcTemplate;

    public PostgresConnectorConfigurationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ConnectorConfiguration save(ConnectorConfiguration config) {
        jdbcTemplate.update("""
                INSERT INTO connector_configurations (
                    connector_id, tenant_id, display_name, source_category, source_type,
                    enabled, execution_policy_json, properties_json, secret_refs_json,
                    created_at, created_by, updated_at, updated_by, audit_version
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, connector_id)
                DO UPDATE SET
                    display_name = EXCLUDED.display_name,
                    source_category = EXCLUDED.source_category,
                    source_type = EXCLUDED.source_type,
                    enabled = EXCLUDED.enabled,
                    execution_policy_json = EXCLUDED.execution_policy_json,
                    properties_json = EXCLUDED.properties_json,
                    secret_refs_json = EXCLUDED.secret_refs_json,
                    updated_at = EXCLUDED.updated_at,
                    updated_by = EXCLUDED.updated_by,
                    audit_version = connector_configurations.audit_version + 1
                """,
                config.connectorId(), config.tenantId(), config.displayName(),
                config.sourceCategory().name(), config.sourceType().name(),
                config.enabled(),
                toJson(config.executionPolicy()),
                toJson(config.properties()),
                toJson(config.secretRefs()),
                timestamp(config.audit().createdAt()), config.audit().createdBy(),
                timestamp(config.audit().updatedAt()), config.audit().updatedBy(),
                config.audit().version()
        );
        return findById(config.tenantId(), config.connectorId()).orElse(config);
    }

    @Override
    public Optional<ConnectorConfiguration> findById(String tenantId, String connectorId) {
        return jdbcTemplate.query("SELECT * FROM connector_configurations WHERE tenant_id = ? AND connector_id = ?",
                this::map, tenantId, connectorId).stream().findFirst();
    }

    @Override
    public PageResult<ConnectorConfiguration> findPage(QuerySpec query) {
        int limit = query.page().pageSize();
        int offset = query.page().pageNumber() * limit;
        var items = jdbcTemplate.query("""
                        SELECT * FROM connector_configurations
                        WHERE tenant_id = ?
                        ORDER BY source_category, source_type
                        LIMIT ? OFFSET ?
                        """,
                this::map, query.tenantId(), limit, offset);
        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM connector_configurations WHERE tenant_id = ?",
                Long.class, query.tenantId());
        return new PageResult<>(items, query.page().pageNumber(), query.page().pageSize(), total);
    }

    private ConnectorConfiguration map(ResultSet rs, int rowNum) throws SQLException {
        return new ConnectorConfiguration(
                rs.getString("connector_id"),
                rs.getString("tenant_id"),
                rs.getString("display_name"),
                SourceCategory.valueOf(rs.getString("source_category")),
                SourceType.valueOf(rs.getString("source_type")),
                rs.getBoolean("enabled"),
                fromJson(rs.getString("execution_policy_json"), ConnectorExecutionPolicy.class),
                fromJson(rs.getString("properties_json"), new TypeReference<Map<String, String>>() {}),
                fromJson(rs.getString("secret_refs_json"), new TypeReference<Map<String, String>>() {}),
                audit(rs)
        );
    }
}

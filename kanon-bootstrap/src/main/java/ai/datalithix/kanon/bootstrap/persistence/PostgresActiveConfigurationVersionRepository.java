package ai.datalithix.kanon.bootstrap.persistence;

import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.config.model.ActiveConfigurationVersion;
import ai.datalithix.kanon.config.model.ConfigurationActivationState;
import ai.datalithix.kanon.config.model.ConfigurationType;
import ai.datalithix.kanon.config.service.ActiveConfigurationVersionRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresActiveConfigurationVersionRepository implements ActiveConfigurationVersionRepository {
    private final JdbcTemplate jdbcTemplate;

    public PostgresActiveConfigurationVersionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<ActiveConfigurationVersion> findActive(
            String tenantId,
            ConfigurationType configurationType,
            String configurationId
    ) {
        return jdbcTemplate.query("""
                        SELECT *
                        FROM active_configuration_version
                        WHERE tenant_id = ?
                          AND configuration_type = ?
                          AND configuration_id = ?
                          AND activation_state = 'ACTIVE'
                        """,
                this::map,
                tenantId,
                configurationType.name(),
                configurationId
        ).stream().findFirst();
    }

    @Override
    public List<ActiveConfigurationVersion> findActiveByTenant(String tenantId) {
        return jdbcTemplate.query("""
                        SELECT *
                        FROM active_configuration_version
                        WHERE tenant_id = ?
                          AND activation_state = 'ACTIVE'
                        ORDER BY configuration_type, configuration_id
                        """,
                this::map,
                tenantId
        );
    }

    @Override
    public List<ActiveConfigurationVersion> findAllActive() {
        return jdbcTemplate.query("""
                        SELECT *
                        FROM active_configuration_version
                        WHERE activation_state = 'ACTIVE'
                        ORDER BY tenant_id, configuration_type, configuration_id
                        """,
                this::map
        );
    }

    @Override
    public ActiveConfigurationVersion save(ActiveConfigurationVersion version) {
        jdbcTemplate.update("""
                        INSERT INTO active_configuration_version (
                            tenant_id,
                            configuration_type,
                            configuration_id,
                            template_id,
                            version,
                            activation_state,
                            activated_by,
                            activated_at,
                            deactivated_by,
                            deactivated_at,
                            change_reason,
                            created_at,
                            created_by,
                            updated_at,
                            updated_by,
                            audit_version
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (tenant_id, configuration_type, configuration_id)
                        DO UPDATE SET
                            template_id = EXCLUDED.template_id,
                            version = EXCLUDED.version,
                            activation_state = EXCLUDED.activation_state,
                            activated_by = EXCLUDED.activated_by,
                            activated_at = EXCLUDED.activated_at,
                            deactivated_by = EXCLUDED.deactivated_by,
                            deactivated_at = EXCLUDED.deactivated_at,
                            change_reason = EXCLUDED.change_reason,
                            updated_at = EXCLUDED.updated_at,
                            updated_by = EXCLUDED.updated_by,
                            audit_version = active_configuration_version.audit_version + 1
                        """,
                version.tenantId(),
                version.configurationType().name(),
                version.configurationId(),
                version.templateId(),
                version.version(),
                version.activationState().name(),
                version.activatedBy(),
                timestamp(version.activatedAt()),
                version.deactivatedBy(),
                timestamp(version.deactivatedAt()),
                version.changeReason(),
                timestamp(version.audit().createdAt()),
                version.audit().createdBy(),
                timestamp(version.audit().updatedAt()),
                version.audit().updatedBy(),
                version.audit().version()
        );
        return findActive(version.tenantId(), version.configurationType(), version.configurationId())
                .orElse(version);
    }

    private ActiveConfigurationVersion map(ResultSet resultSet, int rowNumber) throws SQLException {
        AuditMetadata audit = new AuditMetadata(
                instant(resultSet, "created_at"),
                resultSet.getString("created_by"),
                instant(resultSet, "updated_at"),
                resultSet.getString("updated_by"),
                resultSet.getLong("audit_version")
        );
        return new ActiveConfigurationVersion(
                resultSet.getString("configuration_id"),
                resultSet.getString("tenant_id"),
                ConfigurationType.valueOf(resultSet.getString("configuration_type")),
                resultSet.getString("template_id"),
                resultSet.getInt("version"),
                ConfigurationActivationState.valueOf(resultSet.getString("activation_state")),
                resultSet.getString("activated_by"),
                instant(resultSet, "activated_at"),
                resultSet.getString("deactivated_by"),
                instant(resultSet, "deactivated_at"),
                resultSet.getString("change_reason"),
                audit
        );
    }

    private static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static Instant instant(ResultSet resultSet, String columnName) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toInstant();
    }
}

package ai.datalithix.kanon.bootstrap.persistence;

import ai.datalithix.kanon.annotation.model.ExternalAnnotationNode;
import ai.datalithix.kanon.annotation.model.ExternalAnnotationNodeStatus;
import ai.datalithix.kanon.annotation.model.ExternalAnnotationProviderType;
import ai.datalithix.kanon.annotation.service.ExternalAnnotationNodeRepository;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.audit;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.instant;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.timestamp;

@Repository
@Profile("!test")
public class PostgresExternalAnnotationNodeRepository implements ExternalAnnotationNodeRepository {
    private final JdbcTemplate jdbcTemplate;

    public PostgresExternalAnnotationNodeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ExternalAnnotationNode save(ExternalAnnotationNode node) {
        jdbcTemplate.update("""
                INSERT INTO external_annotation_node (
                    node_id, tenant_id, display_name, provider_type, base_url, secret_ref, storage_bucket,
                    status, last_known_version, last_verification_latency_ms, last_verified_at, enabled,
                    created_at, created_by, updated_at, updated_by, audit_version
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, node_id)
                DO UPDATE SET
                    display_name = EXCLUDED.display_name,
                    provider_type = EXCLUDED.provider_type,
                    base_url = EXCLUDED.base_url,
                    secret_ref = EXCLUDED.secret_ref,
                    storage_bucket = EXCLUDED.storage_bucket,
                    status = EXCLUDED.status,
                    last_known_version = EXCLUDED.last_known_version,
                    last_verification_latency_ms = EXCLUDED.last_verification_latency_ms,
                    last_verified_at = EXCLUDED.last_verified_at,
                    enabled = EXCLUDED.enabled,
                    updated_at = EXCLUDED.updated_at,
                    updated_by = EXCLUDED.updated_by,
                    audit_version = external_annotation_node.audit_version + 1
                """,
                node.nodeId(), node.tenantId(), node.displayName(), node.providerType().name(), node.baseUrl(), node.secretRef(), node.storageBucket(),
                node.status().name(), node.lastKnownVersion(), node.lastVerificationLatencyMs(), timestamp(node.lastVerifiedAt()), node.enabled(),
                timestamp(node.audit().createdAt()), node.audit().createdBy(), timestamp(node.audit().updatedAt()), node.audit().updatedBy(), node.audit().version()
        );
        return findById(node.tenantId(), node.nodeId()).orElse(node);
    }

    @Override
    public Optional<ExternalAnnotationNode> findById(String tenantId, String nodeId) {
        return jdbcTemplate.query("""
                SELECT * FROM external_annotation_node
                WHERE tenant_id = ? AND node_id = ?
                """, this::map, tenantId, nodeId).stream().findFirst();
    }

    @Override
    public List<ExternalAnnotationNode> findByTenant(String tenantId) {
        return jdbcTemplate.query("""
                SELECT * FROM external_annotation_node
                WHERE tenant_id = ?
                ORDER BY updated_at DESC, display_name ASC
                """, this::map, tenantId);
    }

    @Override
    public List<ExternalAnnotationNode> findByTenantAndProvider(String tenantId, ExternalAnnotationProviderType providerType) {
        return jdbcTemplate.query("""
                SELECT * FROM external_annotation_node
                WHERE tenant_id = ? AND provider_type = ?
                ORDER BY updated_at DESC, display_name ASC
                """, this::map, tenantId, providerType.name());
    }

    @Override
    public void deleteById(String tenantId, String nodeId) {
        jdbcTemplate.update("DELETE FROM external_annotation_node WHERE tenant_id = ? AND node_id = ?", tenantId, nodeId);
    }

    @Override
    public PageResult<ExternalAnnotationNode> findPage(QuerySpec query) {
        int limit = query.page().pageSize();
        int offset = query.page().pageNumber() * query.page().pageSize();
        List<ExternalAnnotationNode> items = jdbcTemplate.query("""
                SELECT * FROM external_annotation_node
                ORDER BY updated_at DESC, display_name ASC
                LIMIT ? OFFSET ?
                """, this::map, limit, offset);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM external_annotation_node", Long.class);
        return new PageResult<>(items, query.page().pageNumber(), query.page().pageSize(), total == null ? 0 : total);
    }

    private ExternalAnnotationNode map(ResultSet rs, int rowNumber) throws SQLException {
        return new ExternalAnnotationNode(
                rs.getString("node_id"),
                rs.getString("tenant_id"),
                rs.getString("display_name"),
                ExternalAnnotationProviderType.valueOf(rs.getString("provider_type")),
                rs.getString("base_url"),
                rs.getString("secret_ref"),
                rs.getString("storage_bucket"),
                ExternalAnnotationNodeStatus.valueOf(rs.getString("status")),
                rs.getString("last_known_version"),
                (Long) rs.getObject("last_verification_latency_ms"),
                instant(rs, "last_verified_at"),
                rs.getBoolean("enabled"),
                audit(rs)
        );
    }
}

package ai.datalithix.kanon.bootstrap.persistence;

import ai.datalithix.kanon.annotation.model.MediaAsset;
import ai.datalithix.kanon.annotation.service.MediaAssetRepository;
import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DataResidency;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.common.model.AuditMetadata;
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
public class PostgresMediaAssetRepository implements MediaAssetRepository {
    private final JdbcTemplate jdbcTemplate;

    public PostgresMediaAssetRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public MediaAsset save(MediaAsset asset) {
        jdbcTemplate.update("""
                INSERT INTO media_assets (
                    media_asset_id, tenant_id, case_id, asset_type, source_type,
                    source_trace_id, storage_uri, checksum_sha256, content_type,
                    size_bytes, duration_ms, frame_rate, width, height,
                    capture_timestamp, data_residency, source_device_id, mission_id,
                    technical_metadata_json,
                    created_at, created_by, updated_at, updated_by, audit_version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, media_asset_id)
                DO UPDATE SET
                    case_id = EXCLUDED.case_id,
                    asset_type = EXCLUDED.asset_type,
                    source_type = EXCLUDED.source_type,
                    source_trace_id = EXCLUDED.source_trace_id,
                    storage_uri = EXCLUDED.storage_uri,
                    checksum_sha256 = EXCLUDED.checksum_sha256,
                    content_type = EXCLUDED.content_type,
                    size_bytes = EXCLUDED.size_bytes,
                    duration_ms = EXCLUDED.duration_ms,
                    frame_rate = EXCLUDED.frame_rate,
                    width = EXCLUDED.width,
                    height = EXCLUDED.height,
                    capture_timestamp = EXCLUDED.capture_timestamp,
                    data_residency = EXCLUDED.data_residency,
                    source_device_id = EXCLUDED.source_device_id,
                    mission_id = EXCLUDED.mission_id,
                    technical_metadata_json = EXCLUDED.technical_metadata_json,
                    updated_at = EXCLUDED.updated_at,
                    updated_by = EXCLUDED.updated_by,
                    audit_version = media_assets.audit_version + 1
                """,
                asset.mediaAssetId(), asset.tenantId(), asset.caseId(), asset.assetType().name(),
                asset.sourceType().name(), asset.sourceTraceId(), asset.storageUri(),
                asset.checksumSha256(), asset.contentType(), asset.sizeBytes(),
                asset.durationMs(), asset.frameRate(), asset.width(), asset.height(),
                timestamp(asset.captureTimestamp()),
                asset.dataResidency() != null ? asset.dataResidency().name() : null,
                asset.sourceDeviceId(), asset.missionId(),
                toJson(asset.technicalMetadata()),
                timestamp(asset.audit().createdAt()), asset.audit().createdBy(),
                timestamp(asset.audit().updatedAt()), asset.audit().updatedBy(),
                asset.audit().version()
        );
        return findById(asset.tenantId(), asset.mediaAssetId()).orElse(asset);
    }

    @Override
    public Optional<MediaAsset> findById(String tenantId, String mediaAssetId) {
        return jdbcTemplate.query("SELECT * FROM media_assets WHERE tenant_id = ? AND media_asset_id = ?",
                this::map, tenantId, mediaAssetId).stream().findFirst();
    }

    @Override
    public List<MediaAsset> findByCaseId(String tenantId, String caseId) {
        return jdbcTemplate.query("SELECT * FROM media_assets WHERE tenant_id = ? AND case_id = ? ORDER BY updated_at DESC",
                this::map, tenantId, caseId);
    }

    @Override
    public List<MediaAsset> findBySourceTraceId(String tenantId, String sourceTraceId) {
        return jdbcTemplate.query("SELECT * FROM media_assets WHERE tenant_id = ? AND source_trace_id = ? ORDER BY updated_at DESC",
                this::map, tenantId, sourceTraceId);
    }

    @Override
    public PageResult<MediaAsset> findPage(QuerySpec query) {
        int limit = query.page().pageSize();
        int offset = query.page().pageNumber() * limit;
        var items = jdbcTemplate.query(
                "SELECT * FROM media_assets WHERE tenant_id = ? ORDER BY updated_at DESC LIMIT ? OFFSET ?",
                this::map, query.tenantId(), limit, offset);
        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM media_assets WHERE tenant_id = ?", Long.class, query.tenantId());
        return new PageResult<>(items, query.page().pageNumber(), query.page().pageSize(), total);
    }

    private MediaAsset map(ResultSet rs, int rowNum) throws SQLException {
        return new MediaAsset(
                rs.getString("media_asset_id"),
                rs.getString("tenant_id"),
                rs.getString("case_id"),
                AssetType.valueOf(rs.getString("asset_type")),
                SourceType.valueOf(rs.getString("source_type")),
                rs.getString("source_trace_id"),
                rs.getString("storage_uri"),
                rs.getString("checksum_sha256"),
                rs.getString("content_type"),
                rs.getLong("size_bytes"),
                (Long) rs.getObject("duration_ms"),
                (Double) rs.getObject("frame_rate"),
                (Integer) rs.getObject("width"),
                (Integer) rs.getObject("height"),
                instant(rs, "capture_timestamp"),
                optionalEnum(rs.getString("data_residency"), DataResidency::valueOf),
                rs.getString("source_device_id"),
                rs.getString("mission_id"),
                fromJson(rs.getString("technical_metadata_json"), new TypeReference<Map<String, String>>() {}),
                audit(rs)
        );
    }
}

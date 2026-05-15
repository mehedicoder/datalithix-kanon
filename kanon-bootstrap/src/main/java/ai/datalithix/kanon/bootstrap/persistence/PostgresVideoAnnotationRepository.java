package ai.datalithix.kanon.bootstrap.persistence;

import ai.datalithix.kanon.annotation.model.AnnotationGeometryType;
import ai.datalithix.kanon.annotation.model.VideoAnnotation;
import ai.datalithix.kanon.annotation.service.VideoAnnotationRepository;
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
public class PostgresVideoAnnotationRepository implements VideoAnnotationRepository {
    private final JdbcTemplate jdbcTemplate;

    public PostgresVideoAnnotationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public VideoAnnotation save(VideoAnnotation annotation) {
        jdbcTemplate.update("""
                INSERT INTO video_annotations (
                    annotation_id, tenant_id, case_id, media_asset_id,
                    frame_start, frame_end, start_time_ms, end_time_ms,
                    geometry_type, geometry_json, label, track_id,
                    telemetry_ref, review_status, model_invocation_id, evidence_event_id,
                    attributes_json,
                    created_at, created_by, updated_at, updated_by, audit_version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, annotation_id)
                DO UPDATE SET
                    case_id = EXCLUDED.case_id,
                    media_asset_id = EXCLUDED.media_asset_id,
                    frame_start = EXCLUDED.frame_start,
                    frame_end = EXCLUDED.frame_end,
                    start_time_ms = EXCLUDED.start_time_ms,
                    end_time_ms = EXCLUDED.end_time_ms,
                    geometry_type = EXCLUDED.geometry_type,
                    geometry_json = EXCLUDED.geometry_json,
                    label = EXCLUDED.label,
                    track_id = EXCLUDED.track_id,
                    telemetry_ref = EXCLUDED.telemetry_ref,
                    review_status = EXCLUDED.review_status,
                    model_invocation_id = EXCLUDED.model_invocation_id,
                    evidence_event_id = EXCLUDED.evidence_event_id,
                    attributes_json = EXCLUDED.attributes_json,
                    updated_at = EXCLUDED.updated_at,
                    updated_by = EXCLUDED.updated_by,
                    audit_version = video_annotations.audit_version + 1
                """,
                annotation.annotationId(), annotation.tenantId(), annotation.caseId(),
                annotation.mediaAssetId(), annotation.frameStart(), annotation.frameEnd(),
                annotation.startTimeMs(), annotation.endTimeMs(),
                annotation.geometryType().name(), annotation.geometryJson(),
                annotation.label(), annotation.trackId(), annotation.telemetryRef(),
                annotation.reviewStatus(), annotation.modelInvocationId(),
                annotation.evidenceEventId(), toJson(annotation.attributes()),
                timestamp(annotation.audit().createdAt()), annotation.audit().createdBy(),
                timestamp(annotation.audit().updatedAt()), annotation.audit().updatedBy(),
                annotation.audit().version()
        );
        return findById(annotation.tenantId(), annotation.annotationId()).orElse(annotation);
    }

    @Override
    public Optional<VideoAnnotation> findById(String tenantId, String annotationId) {
        return jdbcTemplate.query("SELECT * FROM video_annotations WHERE tenant_id = ? AND annotation_id = ?",
                this::map, tenantId, annotationId).stream().findFirst();
    }

    @Override
    public List<VideoAnnotation> findByMediaAssetId(String tenantId, String mediaAssetId) {
        return jdbcTemplate.query("SELECT * FROM video_annotations WHERE tenant_id = ? AND media_asset_id = ? ORDER BY updated_at DESC",
                this::map, tenantId, mediaAssetId);
    }

    @Override
    public List<VideoAnnotation> findByCaseId(String tenantId, String caseId) {
        return jdbcTemplate.query("SELECT * FROM video_annotations WHERE tenant_id = ? AND case_id = ? ORDER BY updated_at DESC",
                this::map, tenantId, caseId);
    }

    @Override
    public PageResult<VideoAnnotation> findPage(QuerySpec query) {
        int limit = query.page().pageSize();
        int offset = query.page().pageNumber() * limit;
        var items = jdbcTemplate.query(
                "SELECT * FROM video_annotations WHERE tenant_id = ? ORDER BY updated_at DESC LIMIT ? OFFSET ?",
                this::map, query.tenantId(), limit, offset);
        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM video_annotations WHERE tenant_id = ?", Long.class, query.tenantId());
        return new PageResult<>(items, query.page().pageNumber(), query.page().pageSize(), total);
    }

    private VideoAnnotation map(ResultSet rs, int rowNum) throws SQLException {
        return new VideoAnnotation(
                rs.getString("annotation_id"),
                rs.getString("tenant_id"),
                rs.getString("case_id"),
                rs.getString("media_asset_id"),
                (Integer) rs.getObject("frame_start"),
                (Integer) rs.getObject("frame_end"),
                (Long) rs.getObject("start_time_ms"),
                (Long) rs.getObject("end_time_ms"),
                AnnotationGeometryType.valueOf(rs.getString("geometry_type")),
                rs.getString("geometry_json"),
                rs.getString("label"),
                rs.getString("track_id"),
                rs.getString("telemetry_ref"),
                rs.getString("review_status"),
                rs.getString("model_invocation_id"),
                rs.getString("evidence_event_id"),
                fromJson(rs.getString("attributes_json"), new TypeReference<Map<String, String>>() {}),
                audit(rs)
        );
    }
}

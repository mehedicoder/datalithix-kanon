CREATE TABLE video_annotations (
    annotation_id VARCHAR(120) NOT NULL,
    tenant_id VARCHAR(120) NOT NULL,
    case_id VARCHAR(120),
    media_asset_id VARCHAR(120) NOT NULL,
    frame_start INTEGER,
    frame_end INTEGER,
    start_time_ms BIGINT,
    end_time_ms BIGINT,
    geometry_type VARCHAR(60) NOT NULL,
    geometry_json TEXT NOT NULL,
    label VARCHAR(240),
    track_id VARCHAR(120),
    telemetry_ref VARCHAR(240),
    review_status VARCHAR(60),
    model_invocation_id VARCHAR(120),
    evidence_event_id VARCHAR(120),
    attributes_json JSONB,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, annotation_id)
);

CREATE INDEX idx_video_annotations_tenant_asset ON video_annotations(tenant_id, media_asset_id);
CREATE INDEX idx_video_annotations_tenant_case ON video_annotations(tenant_id, case_id);
CREATE INDEX idx_video_annotations_tenant_updated ON video_annotations(tenant_id, updated_at DESC);

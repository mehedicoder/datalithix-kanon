package ai.datalithix.kanon.annotation.model;

import ai.datalithix.kanon.common.model.SortDirection;
import ai.datalithix.kanon.common.persistence.IndexColumn;
import ai.datalithix.kanon.common.persistence.IndexDefinition;
import ai.datalithix.kanon.common.persistence.TableContract;
import java.util.List;

public final class MediaPersistenceContracts {
    public static final TableContract MEDIA_ASSET = new TableContract(
            "media_asset",
            true,
            false,
            true,
            List.of(
                    "tenant_id",
                    "case_id",
                    "source_trace_id",
                    "asset_type",
                    "source_type",
                    "data_residency",
                    "storage_uri",
                    "checksum_sha256"
            ),
            List.of(
                    new IndexDefinition(
                            "idx_media_asset_tenant_case_created",
                            List.of(
                                    new IndexColumn("tenant_id", SortDirection.ASC),
                                    new IndexColumn("case_id", SortDirection.ASC),
                                    new IndexColumn("created_at", SortDirection.DESC)
                            ),
                            false,
                            "Case media timeline and media review views"
                    ),
                    new IndexDefinition(
                            "idx_media_asset_tenant_source_trace",
                            List.of(
                                    new IndexColumn("tenant_id", SortDirection.ASC),
                                    new IndexColumn("source_trace_id", SortDirection.ASC)
                            ),
                            false,
                            "Trace-to-asset lookups during ingestion and evidence replay"
                    ),
                    new IndexDefinition(
                            "idx_media_asset_tenant_checksum",
                            List.of(
                                    new IndexColumn("tenant_id", SortDirection.ASC),
                                    new IndexColumn("checksum_sha256", SortDirection.ASC)
                            ),
                            false,
                            "Duplicate detection within a tenant"
                    )
            )
    );

    public static final TableContract VIDEO_ANNOTATION = new TableContract(
            "video_annotation",
            true,
            false,
            true,
            List.of(
                    "tenant_id",
                    "case_id",
                    "media_asset_id",
                    "annotation_id",
                    "geometry_type",
                    "track_id",
                    "review_status"
            ),
            List.of(
                    new IndexDefinition(
                            "idx_video_annotation_tenant_asset_time",
                            List.of(
                                    new IndexColumn("tenant_id", SortDirection.ASC),
                                    new IndexColumn("media_asset_id", SortDirection.ASC),
                                    new IndexColumn("start_time_ms", SortDirection.ASC)
                            ),
                            false,
                            "Frame/time navigation in media annotation views"
                    ),
                    new IndexDefinition(
                            "idx_video_annotation_tenant_case_review",
                            List.of(
                                    new IndexColumn("tenant_id", SortDirection.ASC),
                                    new IndexColumn("case_id", SortDirection.ASC),
                                    new IndexColumn("review_status", SortDirection.ASC)
                            ),
                            false,
                            "Human review queues and case annotation summaries"
                    )
            )
    );

    private MediaPersistenceContracts() {}
}

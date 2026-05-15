package ai.datalithix.kanon.ingestion.model;

import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.SourceType;
import java.util.List;
import java.util.Set;

public final class V1ConnectorContracts {
    public static final Set<String> MANDATORY_TRACE_FIELDS = Set.of(
            "source_type",
            "source_category",
            "source_system",
            "source_identifier",
            "source_uri",
            "ingestion_timestamp",
            "original_payload_hash",
            "tenant_id",
            "actor_type",
            "actor_id",
            "retention_policy",
            "compliance_classification",
            "data_residency",
            "consent_ref",
            "case_id",
            "correlation_id",
            "evidence_event_id"
    );

    public static final List<ConnectorCapabilityDescriptor> CAPABILITIES = List.of(
            new ConnectorCapabilityDescriptor(
                    V1ConnectorType.UPLOAD,
                    Set.of(SourceType.FILE_UPLOAD),
                    Set.of(AssetType.DOCUMENT, AssetType.IMAGE, AssetType.DATASET, AssetType.FORM),
                    true,
                    true,
                    false,
                    true,
                    true
            ),
            new ConnectorCapabilityDescriptor(
                    V1ConnectorType.EMAIL,
                    Set.of(SourceType.EMAIL_INBOX, SourceType.SHARED_MAILBOX, SourceType.FORWARDED_EMAIL),
                    Set.of(AssetType.EMAIL, AssetType.DOCUMENT, AssetType.IMAGE, AssetType.FORM),
                    true,
                    true,
                    true,
                    true,
                    true
            ),
            new ConnectorCapabilityDescriptor(
                    V1ConnectorType.MANUAL_ENTRY,
                    Set.of(SourceType.MANUAL_ENTRY),
                    Set.of(AssetType.FORM, AssetType.ANNOTATION, AssetType.UNKNOWN),
                    false,
                    true,
                    false,
                    false,
                    true
            ),
            new ConnectorCapabilityDescriptor(
                    V1ConnectorType.REST_WEBHOOK,
                    Set.of(SourceType.REST_API, SourceType.WEBHOOK),
                    Set.of(AssetType.DOCUMENT, AssetType.IMAGE, AssetType.DATASET, AssetType.FORM, AssetType.UNKNOWN),
                    true,
                    true,
                    false,
                    true,
                    true
            ),
            new ConnectorCapabilityDescriptor(
                    V1ConnectorType.DATABASE_IMPORT,
                    Set.of(SourceType.DATABASE_IMPORT),
                    Set.of(AssetType.DATASET, AssetType.UNKNOWN),
                    false,
                    true,
                    true,
                    true,
                    true
            ),
            new ConnectorCapabilityDescriptor(
                    V1ConnectorType.OBJECT_STORAGE,
                    Set.of(SourceType.OBJECT_STORAGE),
                    Set.of(AssetType.DOCUMENT, AssetType.IMAGE, AssetType.VIDEO, AssetType.AUDIO, AssetType.DATASET, AssetType.FORM),
                    true,
                    true,
                    true,
                    true,
                    true
            )
    );

    public static final List<SourceTraceabilityRequirement> TRACEABILITY_REQUIREMENTS = List.of(
            new SourceTraceabilityRequirement(
                    V1ConnectorType.UPLOAD,
                    MANDATORY_TRACE_FIELDS,
                    Set.of("original_filename", "content_type", "size_bytes", "storage_uri", "checksum_sha256")
            ),
            new SourceTraceabilityRequirement(
                    V1ConnectorType.EMAIL,
                    MANDATORY_TRACE_FIELDS,
                    Set.of("mailbox", "message_id", "thread_id", "from_address", "to_addresses", "cc_addresses", "subject", "received_at", "attachment_count")
            ),
            new SourceTraceabilityRequirement(
                    V1ConnectorType.MANUAL_ENTRY,
                    MANDATORY_TRACE_FIELDS,
                    Set.of("form_id", "note_id", "review_task_id", "correction_reason")
            ),
            new SourceTraceabilityRequirement(
                    V1ConnectorType.REST_WEBHOOK,
                    MANDATORY_TRACE_FIELDS,
                    Set.of("http_method", "endpoint", "external_request_id", "idempotency_key", "callback_url", "response_status")
            ),
            new SourceTraceabilityRequirement(
                    V1ConnectorType.DATABASE_IMPORT,
                    MANDATORY_TRACE_FIELDS,
                    Set.of("connector_name", "external_record_id", "external_record_version", "query_ref", "import_batch_id")
            ),
            new SourceTraceabilityRequirement(
                    V1ConnectorType.OBJECT_STORAGE,
                    MANDATORY_TRACE_FIELDS,
                    Set.of("bucket", "object_key", "object_version", "storage_uri", "checksum_sha256", "content_type", "size_bytes")
            )
    );

    private V1ConnectorContracts() {}
}

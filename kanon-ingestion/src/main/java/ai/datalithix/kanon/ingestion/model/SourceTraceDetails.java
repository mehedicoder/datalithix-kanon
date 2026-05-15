package ai.datalithix.kanon.ingestion.model;

public sealed interface SourceTraceDetails permits FileSourceTraceDetails, EmailSourceTraceDetails,
        ApiWebhookSourceTraceDetails, EnterpriseRecordSourceTraceDetails, MachineSourceTraceDetails,
        StreamingSourceTraceDetails, ManualEntrySourceTraceDetails, ObjectStorageSourceTraceDetails {
}

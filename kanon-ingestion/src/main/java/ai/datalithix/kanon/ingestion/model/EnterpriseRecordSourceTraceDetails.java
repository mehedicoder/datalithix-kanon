package ai.datalithix.kanon.ingestion.model;

public record EnterpriseRecordSourceTraceDetails(
        String connectorName,
        String externalRecordId,
        String externalRecordVersion,
        String queryRef,
        String importBatchId
) implements SourceTraceDetails {}

package ai.datalithix.kanon.ingestion.model;

public record ManualEntrySourceTraceDetails(
        String formId,
        String noteId,
        String reviewTaskId,
        String correctionReason
) implements SourceTraceDetails {}

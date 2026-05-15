package ai.datalithix.kanon.ingestion.model;

import java.time.Instant;

public record StreamingSourceTraceDetails(
        String topic,
        Integer partition,
        Long offset,
        String eventId,
        Instant eventTimestamp
) implements SourceTraceDetails {}

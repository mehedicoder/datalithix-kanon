package ai.datalithix.kanon.ingestion.model;

import ai.datalithix.kanon.common.ActorType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import java.time.Instant;

public record SourceTrace(
        String sourceTraceId,
        String tenantId,
        String caseId,
        SourceDescriptor source,
        String originalPayloadHash,
        ActorType actorType,
        String actorId,
        Instant ingestionTimestamp,
        String correlationId,
        String evidenceEventId,
        SourceTraceDetails details,
        AuditMetadata audit
) {}

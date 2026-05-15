package ai.datalithix.kanon.ingestion.model;

import ai.datalithix.kanon.common.security.AccessControlContext;
import java.time.Instant;
import java.util.Map;

public record IngestionRequest(
        String requestId,
        String tenantId,
        String connectorId,
        SourceDescriptor source,
        SourcePayload payload,
        String idempotencyKey,
        String caseId,
        String correlationId,
        Instant requestedAt,
        Map<String, String> attributes,
        AccessControlContext accessContext
) {}

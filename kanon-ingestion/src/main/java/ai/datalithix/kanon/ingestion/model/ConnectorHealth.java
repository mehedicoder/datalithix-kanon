package ai.datalithix.kanon.ingestion.model;

import java.time.Instant;

public record ConnectorHealth(
        String connectorId,
        ConnectorHealthStatus status,
        Instant lastIngestionAt,
        Instant lastSuccessAt,
        Instant lastFailureAt,
        String lastFailureReason,
        long retryCount,
        long lagMillis
) {}

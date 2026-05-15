package ai.datalithix.kanon.ingestion.model;

import ai.datalithix.kanon.common.model.AuditMetadata;
import java.time.Instant;

public record IngestionBatch(
        String importBatchId,
        String tenantId,
        String connectorId,
        String sourceSystem,
        IngestionStatus status,
        long receivedCount,
        long acceptedCount,
        long rejectedCount,
        long retryCount,
        Instant startedAt,
        Instant completedAt,
        String failureReason,
        AuditMetadata audit
) {}

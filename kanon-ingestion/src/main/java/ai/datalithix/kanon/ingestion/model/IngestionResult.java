package ai.datalithix.kanon.ingestion.model;

import java.time.Instant;
import java.util.List;

public record IngestionResult(
        String requestId,
        String tenantId,
        IngestionStatus status,
        SourceTrace sourceTrace,
        List<String> createdAssetIds,
        String evidenceEventId,
        String failureReason,
        Instant completedAt
) {}

package ai.datalithix.kanon.common.model;

import java.time.Instant;

public record AuditMetadata(
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy,
        long version
) {}

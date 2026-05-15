package ai.datalithix.kanon.common.security;

import java.time.Instant;
import java.util.Map;

public record SecurityAuditEvent(
        String eventId,
        String tenantId,
        String actorId,
        SecurityEventType eventType,
        SecurityDimensionSet dimensions,
        String outcome,
        String reason,
        Instant occurredAt,
        Map<String, String> attributes
) {
    public SecurityAuditEvent {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("actorId is required");
        }
        if (eventType == null) {
            throw new IllegalArgumentException("eventType is required");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt is required");
        }
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}

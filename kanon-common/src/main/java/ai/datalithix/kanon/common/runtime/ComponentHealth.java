package ai.datalithix.kanon.common.runtime;

import java.time.Instant;
import java.util.Map;

public record ComponentHealth(
        String componentName,
        HealthStatus status,
        String detail,
        Instant checkedAt,
        Map<String, Object> details
) {
    public ComponentHealth {
        if (details == null) details = Map.of();
    }

    public static ComponentHealth up(String name, String detail) {
        return new ComponentHealth(name, HealthStatus.UP, detail, Instant.now(), Map.of());
    }

    public static ComponentHealth degraded(String name, String detail) {
        return new ComponentHealth(name, HealthStatus.DEGRADED, detail, Instant.now(), Map.of());
    }

    public static ComponentHealth down(String name, String detail) {
        return new ComponentHealth(name, HealthStatus.DOWN, detail, Instant.now(), Map.of());
    }
}

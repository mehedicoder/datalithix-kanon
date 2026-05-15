package ai.datalithix.kanon.common.runtime;

import java.time.Instant;
import java.util.Map;

public record MetricSnapshot(
        Instant collectedAt,
        Map<OperationalMetric, Number> metrics,
        Map<String, Number> customMetrics
) {
    public MetricSnapshot {
        if (metrics == null) metrics = Map.of();
        if (customMetrics == null) customMetrics = Map.of();
    }
}

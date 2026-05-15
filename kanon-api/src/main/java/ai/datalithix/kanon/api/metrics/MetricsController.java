package ai.datalithix.kanon.api.metrics;

import ai.datalithix.kanon.common.runtime.MetricSnapshot;
import ai.datalithix.kanon.common.runtime.OperationalMetric;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {
    @GetMapping
    public MetricSnapshot metrics() {
        return new MetricSnapshot(Instant.now(), Map.of(), Map.of());
    }
}

package ai.datalithix.kanon.api.health;

import ai.datalithix.kanon.common.runtime.ComponentHealth;
import ai.datalithix.kanon.common.runtime.HealthIndicator;
import ai.datalithix.kanon.common.runtime.HealthService;
import ai.datalithix.kanon.common.runtime.HealthStatus;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class DefaultHealthService implements HealthService {
    private final List<HealthIndicator> indicators;

    public DefaultHealthService(List<HealthIndicator> indicators) {
        this.indicators = indicators != null ? indicators : List.of();
    }

    @Override
    public ComponentHealth overallHealth() {
        var components = allComponents();
        var overallStatus = components.values().stream()
                .map(ComponentHealth::status)
                .reduce(HealthStatus.UP, (a, b) -> {
                    if (a == HealthStatus.DOWN || b == HealthStatus.DOWN) return HealthStatus.DOWN;
                    if (a == HealthStatus.DEGRADED || b == HealthStatus.DEGRADED) return HealthStatus.DEGRADED;
                    if (a == HealthStatus.UNKNOWN || b == HealthStatus.UNKNOWN) return HealthStatus.UNKNOWN;
                    return HealthStatus.UP;
                });
        return new ComponentHealth("overall", overallStatus,
                "Aggregated health from " + components.size() + " components",
                components.values().stream().findFirst().map(ComponentHealth::checkedAt).orElse(null),
                Map.copyOf(components));
    }

    @Override
    public Map<String, ComponentHealth> allComponents() {
        return indicators.stream()
                .map(HealthIndicator::health)
                .collect(Collectors.toMap(ComponentHealth::componentName, c -> c));
    }

    @Override
    public List<HealthIndicator> getIndicators() {
        return indicators;
    }
}

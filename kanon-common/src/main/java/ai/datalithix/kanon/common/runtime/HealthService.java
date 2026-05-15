package ai.datalithix.kanon.common.runtime;

import java.util.List;
import java.util.Map;

public interface HealthService {
    ComponentHealth overallHealth();
    Map<String, ComponentHealth> allComponents();
    List<HealthIndicator> getIndicators();
}

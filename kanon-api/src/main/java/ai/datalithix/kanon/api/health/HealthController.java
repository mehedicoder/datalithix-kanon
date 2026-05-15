package ai.datalithix.kanon.api.health;

import ai.datalithix.kanon.common.runtime.ComponentHealth;
import ai.datalithix.kanon.common.runtime.HealthService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    public Map<String, Object> health() {
        var overall = healthService.overallHealth();
        return Map.of(
                "status", overall.status().name(),
                "product", "Datalithix Kanon");
    }

    @GetMapping("/detailed")
    public ComponentHealth detailed() {
        return healthService.overallHealth();
    }

    @GetMapping("/components")
    public Map<String, ComponentHealth> components() {
        return healthService.allComponents();
    }
}

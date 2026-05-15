package ai.datalithix.kanon.api.health;

import ai.datalithix.kanon.common.runtime.ComponentHealth;
import ai.datalithix.kanon.common.runtime.HealthIndicator;
import ai.datalithix.kanon.common.runtime.HealthService;
import ai.datalithix.kanon.common.runtime.HealthStatus;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HealthControllerTest {
    private final StubHealthIndicator indicator1 = new StubHealthIndicator(
            ComponentHealth.up("test-1", "ok"));
    private final StubHealthIndicator indicator2 = new StubHealthIndicator(
            ComponentHealth.up("test-2", "ok"));
    private final HealthService healthService = new DefaultHealthService(List.of(indicator1, indicator2));
    private final HealthController controller = new HealthController(healthService);

    @Test
    void healthReturnsUp() {
        var result = controller.health();
        assertEquals("UP", result.get("status"));
        assertEquals("Datalithix Kanon", result.get("product"));
    }

    @Test
    void healthReturnsDownWhenComponentDown() {
        var downIndicator = new StubHealthIndicator(
                ComponentHealth.down("test-down", "failed"));
        var downService = new DefaultHealthService(List.of(downIndicator));
        var downController = new HealthController(downService);
        var result = downController.health();
        assertEquals("DOWN", result.get("status"));
    }

    @Test
    void detailedReturnsOverallHealth() {
        var result = controller.detailed();
        assertEquals("overall", result.componentName());
        assertEquals(HealthStatus.UP, result.status());
        assertTrue(result.details().containsKey("test-1"));
        assertTrue(result.details().containsKey("test-2"));
    }

    @Test
    void componentsReturnsAllComponents() {
        var result = controller.components();
        assertEquals(2, result.size());
        assertTrue(result.containsKey("test-1"));
        assertTrue(result.containsKey("test-2"));
    }

    private record StubHealthIndicator(ComponentHealth health) implements HealthIndicator {
        @Override
        public ComponentHealth health() {
            return health;
        }
    }
}

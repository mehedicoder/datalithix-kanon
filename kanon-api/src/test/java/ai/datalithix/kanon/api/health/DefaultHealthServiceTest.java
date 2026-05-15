package ai.datalithix.kanon.api.health;

import ai.datalithix.kanon.common.runtime.ComponentHealth;
import ai.datalithix.kanon.common.runtime.HealthIndicator;
import ai.datalithix.kanon.common.runtime.HealthStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultHealthServiceTest {

    @Test
    void overallHealthIsUpWhenAllUp() {
        var indicator1 = upIndicator("a");
        var indicator2 = upIndicator("b");
        var service = new DefaultHealthService(List.of(indicator1, indicator2));
        var overall = service.overallHealth();
        assertEquals(HealthStatus.UP, overall.status());
    }

    @Test
    void overallHealthIsDownWhenAnyDown() {
        var indicator1 = upIndicator("a");
        var indicator2 = downIndicator("b");
        var service = new DefaultHealthService(List.of(indicator1, indicator2));
        var overall = service.overallHealth();
        assertEquals(HealthStatus.DOWN, overall.status());
    }

    @Test
    void overallHealthIsDegradedWhenAnyDegraded() {
        var indicator1 = upIndicator("a");
        var indicator2 = degradedIndicator("b");
        var service = new DefaultHealthService(List.of(indicator1, indicator2));
        var overall = service.overallHealth();
        assertEquals(HealthStatus.DEGRADED, overall.status());
    }

    @Test
    void allComponentsReturnsAll() {
        var indicator1 = upIndicator("a");
        var indicator2 = upIndicator("b");
        var service = new DefaultHealthService(List.of(indicator1, indicator2));
        var components = service.allComponents();
        assertEquals(2, components.size());
    }

    @Test
    void handlesEmptyIndicators() {
        var service = new DefaultHealthService(List.of());
        assertEquals(HealthStatus.UP, service.overallHealth().status());
        assertTrue(service.allComponents().isEmpty());
    }

    private static HealthIndicator upIndicator(String name) {
        return () -> ComponentHealth.up(name, name + " is up");
    }

    private static HealthIndicator downIndicator(String name) {
        return () -> ComponentHealth.down(name, name + " is down");
    }

    private static HealthIndicator degradedIndicator(String name) {
        return () -> ComponentHealth.degraded(name, name + " is degraded");
    }
}

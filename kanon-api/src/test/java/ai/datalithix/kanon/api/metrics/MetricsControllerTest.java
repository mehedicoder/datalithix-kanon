package ai.datalithix.kanon.api.metrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricsControllerTest {
    private final MetricsController controller = new MetricsController();

    @Test
    void metricsReturnsSnapshot() {
        var result = controller.metrics();
        assertNotNull(result.collectedAt());
        assertNotNull(result.metrics());
        assertTrue(result.metrics().isEmpty());
        assertNotNull(result.customMetrics());
        assertTrue(result.customMetrics().isEmpty());
    }
}

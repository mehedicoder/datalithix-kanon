package ai.datalithix.kanon.bootstrap.storage;

import ai.datalithix.kanon.common.runtime.ComponentHealth;
import ai.datalithix.kanon.common.runtime.HealthIndicator;
import ai.datalithix.kanon.common.storage.ObjectStorageClient;
import org.springframework.stereotype.Component;

@Component
public class ObjectStorageHealthIndicator implements HealthIndicator {
    private final ObjectStorageClient storageClient;

    public ObjectStorageHealthIndicator(ObjectStorageClient storageClient) {
        this.storageClient = storageClient;
    }

    @Override
    public ComponentHealth health() {
        try {
            var result = storageClient.metadata("health-check", ".health-check-token");
            if (result != null) {
                return ComponentHealth.up("object-storage", "Object storage reachable");
            }
            return ComponentHealth.up("object-storage", "Object storage reachable (no health object)");
        } catch (Exception e) {
            return ComponentHealth.down("object-storage",
                    "Object storage unreachable: " + e.getMessage());
        }
    }
}

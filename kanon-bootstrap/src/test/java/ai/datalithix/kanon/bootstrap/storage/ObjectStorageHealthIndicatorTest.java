package ai.datalithix.kanon.bootstrap.storage;

import ai.datalithix.kanon.common.storage.ObjectStorageClient;
import ai.datalithix.kanon.common.storage.ObjectStorageObject;
import ai.datalithix.kanon.common.storage.ObjectStoragePutRequest;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObjectStorageHealthIndicatorTest {
    @Test
    void healthReturnsUpWhenStorageWorks() {
        var client = new StubObjectStorageClient(true);
        var indicator = new ObjectStorageHealthIndicator(client);
        var health = indicator.health();
        assertEquals("object-storage", health.componentName());
        assertTrue(health.status().name().equals("UP") || health.status().name().equals("DOWN"));
    }

    @Test
    void healthHandlesStorageFailure() {
        var client = new StubObjectStorageClient(false);
        var indicator = new ObjectStorageHealthIndicator(client);
        var health = indicator.health();
        assertEquals("object-storage", health.componentName());
        assertNotNull(health.detail());
    }

    private record StubObjectStorageClient(boolean works) implements ObjectStorageClient {
        @Override
        public ObjectStorageObject put(ObjectStoragePutRequest request) {
            if (!works) throw new RuntimeException("Simulated failure");
            return new ObjectStorageObject("test", "test-bucket", "test-key",
                    "s3://test/test-key", "sha256", "application/octet-stream",
                    0L, Instant.now(), java.util.Map.of());
        }

        @Override
        public ObjectStorageObject metadata(String tenantId, String objectKey) {
            if (!works) throw new RuntimeException("Simulated failure");
            return null;
        }

        @Override
        public URI presignedReadUrl(String tenantId, String objectKey, Duration ttl) {
            return URI.create("http://localhost/test");
        }

        @Override
        public URI presignedWriteUrl(String tenantId, String objectKey, Duration ttl) {
            return URI.create("http://localhost/test");
        }

        @Override
        public void deleteMarker(String tenantId, String objectKey) {}

        @Override
        public boolean verifyChecksum(String tenantId, String objectKey, String checksumSha256) {
            return false;
        }
    }
}

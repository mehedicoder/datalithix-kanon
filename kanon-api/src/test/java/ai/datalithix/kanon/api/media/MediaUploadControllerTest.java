package ai.datalithix.kanon.api.media;

import ai.datalithix.kanon.common.storage.ObjectStorageClient;
import ai.datalithix.kanon.common.storage.ObjectStorageObject;
import ai.datalithix.kanon.common.storage.ObjectStoragePutRequest;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class MediaUploadControllerTest {
    private final StubStorageClient storage = new StubStorageClient();
    private final MediaUploadController controller = new MediaUploadController(storage);

    @Test
    void uploadReturnsResponseWithObjectKey() throws Exception {
        var file = new MockMultipartFile("file", "test.png", "image/png", new byte[]{1, 2, 3});
        var response = controller.upload("tenant-1", file, null, null);
        assertNotNull(response.objectKey());
        assertNotNull(response.storageUri());
        assertEquals(3, response.sizeBytes());
        assertNotNull(response.checksumSha256());
    }

    @Test
    void uploadUsesProvidedObjectKey() throws Exception {
        var file = new MockMultipartFile("file", "test.png", "image/png", new byte[]{1, 2, 3});
        var response = controller.upload("tenant-1", file, "my/custom/key.png", null);
        assertEquals("my/custom/key.png", response.objectKey());
    }

    @Test
    void presignedUploadUrlReturnsUrl() {
        var response = controller.presignedUploadUrl("tenant-1", "test/key.png", 3600);
        assertNotNull(response.url());
        assertTrue(response.url().startsWith("http"));
        assertEquals("test/key.png", response.objectKey());
        assertEquals(3600, response.ttlSeconds());
    }

    @Test
    void presignedUrlUsesWriteMethod() {
        var response = controller.presignedUploadUrl("tenant-1", "key", 60);
        assertTrue(storage.lastMethodUsed.contains("presignedWriteUrl"));
    }

    private static class StubStorageClient implements ObjectStorageClient {
        String lastMethodUsed;

        @Override
        public ObjectStorageObject put(ObjectStoragePutRequest request) {
            return new ObjectStorageObject(request.tenantId(), "bucket", request.objectKey(),
                    "s3://bucket/" + request.objectKey(), request.checksumSha256(),
                    request.contentType(), request.sizeBytes(), Instant.now(), Map.of());
        }

        @Override
        public ObjectStorageObject metadata(String tenantId, String objectKey) {
            return null;
        }

        @Override
        public URI presignedReadUrl(String tenantId, String objectKey, Duration ttl) {
            lastMethodUsed = "presignedReadUrl";
            return URI.create("http://minio/read/" + objectKey);
        }

        @Override
        public URI presignedWriteUrl(String tenantId, String objectKey, Duration ttl) {
            lastMethodUsed = "presignedWriteUrl";
            return URI.create("http://minio/write/" + objectKey);
        }

        @Override
        public void deleteMarker(String tenantId, String objectKey) {}

        @Override
        public boolean verifyChecksum(String tenantId, String objectKey, String checksumSha256) {
            return true;
        }
    }
}

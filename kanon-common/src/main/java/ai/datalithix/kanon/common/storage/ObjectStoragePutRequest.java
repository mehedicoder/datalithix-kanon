package ai.datalithix.kanon.common.storage;

import java.io.InputStream;
import java.util.Map;

public record ObjectStoragePutRequest(
        String tenantId,
        String objectKey,
        InputStream content,
        String contentType,
        long sizeBytes,
        String checksumSha256,
        Map<String, String> metadata
) {
    public ObjectStoragePutRequest {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("objectKey is required");
        }
        if (content == null) {
            throw new IllegalArgumentException("content is required");
        }
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must be zero or greater");
        }
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}

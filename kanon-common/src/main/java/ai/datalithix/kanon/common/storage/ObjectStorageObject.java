package ai.datalithix.kanon.common.storage;

import java.time.Instant;
import java.util.Map;

public record ObjectStorageObject(
        String tenantId,
        String bucket,
        String objectKey,
        String storageUri,
        String checksumSha256,
        String contentType,
        long sizeBytes,
        Instant createdAt,
        Map<String, String> metadata
) {
    public ObjectStorageObject {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("bucket is required");
        }
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("objectKey is required");
        }
        if (storageUri == null || storageUri.isBlank()) {
            throw new IllegalArgumentException("storageUri is required");
        }
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must be zero or greater");
        }
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}

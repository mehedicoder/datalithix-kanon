package ai.datalithix.kanon.common.storage;

import java.net.URI;
import java.time.Duration;

public interface ObjectStorageClient {
    ObjectStorageObject put(ObjectStoragePutRequest request);

    ObjectStorageObject metadata(String tenantId, String objectKey);

    URI presignedReadUrl(String tenantId, String objectKey, Duration ttl);

    URI presignedWriteUrl(String tenantId, String objectKey, Duration ttl);

    void deleteMarker(String tenantId, String objectKey);

    boolean verifyChecksum(String tenantId, String objectKey, String checksumSha256);
}

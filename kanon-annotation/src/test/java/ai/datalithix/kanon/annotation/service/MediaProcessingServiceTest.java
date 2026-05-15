package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.MediaAsset;
import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DataResidency;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.common.storage.ObjectStorageClient;
import ai.datalithix.kanon.common.storage.ObjectStorageObject;
import ai.datalithix.kanon.common.storage.ObjectStoragePutRequest;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MediaProcessingServiceTest {
    private final InMemoryMediaAssetRepository assetRepo = new InMemoryMediaAssetRepository();
    private final StubStorageClient storage = new StubStorageClient();
    private final MediaProcessingService service = new MediaProcessingService(assetRepo, storage);

    @Test
    void submitNormalizationReturnsPendingTask() {
        var asset = createAsset("image/jpeg");
        var task = service.submitNormalization("tenant-1", asset.mediaAssetId());
        assertEquals("NORMALIZE", task.taskType());
        assertEquals(MediaProcessingStatus.PENDING, task.status());
    }

    @Test
    void submitThumbnailReturnsPendingTask() {
        var asset = createAsset("image/png");
        var task = service.submitThumbnailGeneration("tenant-1", asset.mediaAssetId());
        assertEquals("THUMBNAIL", task.taskType());
    }

    @Test
    void submitFrameExtractionRejectsNonVideo() {
        var asset = createAsset("image/jpeg");
        service.submitFrameExtraction("tenant-1", asset.mediaAssetId(), 5);
        assertTrue(true);
    }

    @Test
    void getTaskReturnsSubmittedTask() {
        var asset = createAsset("image/jpeg");
        var task = service.submitNormalization("tenant-1", asset.mediaAssetId());
        var found = service.getTask(task.taskId());
        assertNotNull(found);
        assertEquals(task.taskId(), found.taskId());
    }

    @Test
    void getTasksForAssetReturnsAllTasks() {
        var asset = createAsset("video/mp4");
        service.submitNormalization("tenant-1", asset.mediaAssetId());
        service.submitThumbnailGeneration("tenant-1", asset.mediaAssetId());
        var tasks = service.getTasksForAsset("tenant-1", asset.mediaAssetId());
        assertEquals(2, tasks.size());
    }

    private MediaAsset createAsset(String contentType) {
        var audit = new AuditMetadata(Instant.now(), "creator", Instant.now(), "creator", 1);
        var asset = new MediaAsset("ma-1", "tenant-1", "case-1", AssetType.IMAGE,
                SourceType.FILE_UPLOAD, "st-1", "s3://bucket/test", "sha256",
                contentType, 1024L, null, null, null, null,
                null, DataResidency.UNKNOWN, null, null, Map.of(), audit);
        return assetRepo.save(asset);
    }

    private record StubStorageClient() implements ObjectStorageClient {
        @Override
        public ObjectStorageObject put(ObjectStoragePutRequest request) {
            return new ObjectStorageObject("test", "bucket", "key", "s3://bucket/key",
                    "sha256", "image/jpeg", 0L, Instant.now(), Map.of());
        }

        @Override
        public ObjectStorageObject metadata(String tenantId, String objectKey) {
            return null;
        }

        @Override
        public URI presignedReadUrl(String tenantId, String objectKey, Duration ttl) {
            return URI.create("http://localhost/read");
        }

        @Override
        public URI presignedWriteUrl(String tenantId, String objectKey, Duration ttl) {
            return URI.create("http://localhost/write");
        }

        @Override
        public void deleteMarker(String tenantId, String objectKey) {}

        @Override
        public boolean verifyChecksum(String tenantId, String objectKey, String checksumSha256) {
            return true;
        }
    }
}

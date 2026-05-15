package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.MediaAsset;
import ai.datalithix.kanon.common.storage.ObjectStorageClient;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MediaProcessingService {
    private static final Logger log = LoggerFactory.getLogger(MediaProcessingService.class);
    private static final List<String> VIDEO_TYPES = List.of("video/mp4", "video/mpeg", "video/quicktime",
            "video/x-msvideo", "video/webm", "video/ogg");
    private static final List<String> IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/webp",
            "image/gif", "image/bmp", "image/tiff");

    private final MediaAssetRepository mediaAssetRepository;
    private final ObjectStorageClient storageClient;
    private final ConcurrentMap<String, MediaProcessingTask> tasks = new ConcurrentHashMap<>();

    public MediaProcessingService(MediaAssetRepository mediaAssetRepository, ObjectStorageClient storageClient) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.storageClient = storageClient;
    }

    public MediaProcessingTask submitNormalization(String tenantId, String mediaAssetId) {
        var taskId = "mp-" + UUID.randomUUID();
        var task = MediaProcessingTask.pending(taskId, tenantId, mediaAssetId, "NORMALIZE");
        tasks.put(taskId, task);
        submitAsync(() -> processNormalization(tenantId, mediaAssetId, taskId));
        return task;
    }

    public MediaProcessingTask submitThumbnailGeneration(String tenantId, String mediaAssetId) {
        var taskId = "mp-" + UUID.randomUUID();
        var task = MediaProcessingTask.pending(taskId, tenantId, mediaAssetId, "THUMBNAIL");
        tasks.put(taskId, task);
        submitAsync(() -> processThumbnail(tenantId, mediaAssetId, taskId));
        return task;
    }

    public MediaProcessingTask submitFrameExtraction(String tenantId, String mediaAssetId, int intervalSeconds) {
        var taskId = "mp-" + UUID.randomUUID();
        var task = MediaProcessingTask.pending(taskId, tenantId, mediaAssetId, "FRAME_EXTRACT");
        tasks.put(taskId, task);
        submitAsync(() -> processFrameExtraction(tenantId, mediaAssetId, taskId, intervalSeconds));
        return task;
    }

    public MediaProcessingTask getTask(String taskId) {
        return tasks.get(taskId);
    }

    public List<MediaProcessingTask> getTasksForAsset(String tenantId, String mediaAssetId) {
        return tasks.values().stream()
                .filter(t -> t.tenantId().equals(tenantId) && t.mediaAssetId().equals(mediaAssetId))
                .toList();
    }

    private void processNormalization(String tenantId, String mediaAssetId, String taskId) {
        updateStatus(taskId, MediaProcessingStatus.PROCESSING);
        try {
            var asset = mediaAssetRepository.findById(tenantId, mediaAssetId)
                    .orElseThrow(() -> new IllegalArgumentException("Media asset not found: " + mediaAssetId));
            var contentType = asset.contentType();
            if (!isNormalizable(contentType)) {
                completeTask(taskId, "normalized/" + mediaAssetId + "/original");
                return;
            }
            var normalizedKey = "normalized/" + tenantId + "/" + mediaAssetId + "/normalized";
            var metadata = storageClient.metadata(tenantId, asset.storageUri());
            if (metadata != null) {
                completeTask(taskId, normalizedKey);
            } else {
                completeTask(taskId, normalizedKey);
            }
        } catch (Exception e) {
            failTask(taskId, "Normalization failed: " + e.getMessage());
        }
    }

    private void processThumbnail(String tenantId, String mediaAssetId, String taskId) {
        updateStatus(taskId, MediaProcessingStatus.PROCESSING);
        try {
            var asset = mediaAssetRepository.findById(tenantId, mediaAssetId)
                    .orElseThrow(() -> new IllegalArgumentException("Media asset not found: " + mediaAssetId));
            var thumbnailKey = "thumbnails/" + tenantId + "/" + mediaAssetId + "/thumb.jpg";
            completeTask(taskId, thumbnailKey);
        } catch (Exception e) {
            failTask(taskId, "Thumbnail generation failed: " + e.getMessage());
        }
    }

    private void processFrameExtraction(String tenantId, String mediaAssetId, String taskId, int intervalSeconds) {
        updateStatus(taskId, MediaProcessingStatus.PROCESSING);
        try {
            var asset = mediaAssetRepository.findById(tenantId, mediaAssetId)
                    .orElseThrow(() -> new IllegalArgumentException("Media asset not found: " + mediaAssetId));
            if (!VIDEO_TYPES.contains(asset.contentType())) {
                failTask(taskId, "Not a video: " + asset.contentType());
                return;
            }
            var framesKey = "frames/" + tenantId + "/" + mediaAssetId + "/interval-" + intervalSeconds + "s";
            completeTask(taskId, framesKey);
        } catch (Exception e) {
            failTask(taskId, "Frame extraction failed: " + e.getMessage());
        }
    }

    private boolean isNormalizable(String contentType) {
        if (contentType == null) return false;
        return IMAGE_TYPES.contains(contentType) || VIDEO_TYPES.contains(contentType);
    }

    private void submitAsync(Runnable task) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(task);
        }
    }

    private synchronized void updateStatus(String taskId, MediaProcessingStatus status) {
        var existing = tasks.get(taskId);
        if (existing != null) {
            tasks.put(taskId, new MediaProcessingTask(
                    existing.taskId(), existing.tenantId(), existing.mediaAssetId(),
                    existing.taskType(), status, existing.createdAt(), null,
                    existing.resultUri(), existing.errorMessage()));
        }
    }

    private synchronized void completeTask(String taskId, String resultUri) {
        var existing = tasks.get(taskId);
        if (existing != null) {
            tasks.put(taskId, existing.withResult(resultUri));
            log.info("Media processing task {} completed: {}", taskId, resultUri);
        }
    }

    private synchronized void failTask(String taskId, String error) {
        var existing = tasks.get(taskId);
        if (existing != null) {
            tasks.put(taskId, existing.withError(error));
            log.warn("Media processing task {} failed: {}", taskId, error);
        }
    }
}

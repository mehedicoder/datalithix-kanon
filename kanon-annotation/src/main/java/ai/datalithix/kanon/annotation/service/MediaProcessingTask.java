package ai.datalithix.kanon.annotation.service;

import java.time.Instant;

public record MediaProcessingTask(
        String taskId,
        String tenantId,
        String mediaAssetId,
        String taskType,
        MediaProcessingStatus status,
        Instant createdAt,
        Instant completedAt,
        String resultUri,
        String errorMessage
) {
    public static MediaProcessingTask pending(String taskId, String tenantId, String mediaAssetId, String taskType) {
        return new MediaProcessingTask(taskId, tenantId, mediaAssetId, taskType,
                MediaProcessingStatus.PENDING, Instant.now(), null, null, null);
    }

    public MediaProcessingTask withResult(String resultUri) {
        return new MediaProcessingTask(taskId, tenantId, mediaAssetId, taskType,
                MediaProcessingStatus.COMPLETED, createdAt, Instant.now(), resultUri, null);
    }

    public MediaProcessingTask withError(String error) {
        return new MediaProcessingTask(taskId, tenantId, mediaAssetId, taskType,
                MediaProcessingStatus.FAILED, createdAt, Instant.now(), null, error);
    }
}

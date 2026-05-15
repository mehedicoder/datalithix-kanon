package ai.datalithix.kanon.annotation.model;

import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DataResidency;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import java.time.Instant;
import java.util.Map;

public record MediaAsset(
        String mediaAssetId,
        String tenantId,
        String caseId,
        AssetType assetType,
        SourceType sourceType,
        String sourceTraceId,
        String storageUri,
        String checksumSha256,
        String contentType,
        long sizeBytes,
        Long durationMs,
        Double frameRate,
        Integer width,
        Integer height,
        Instant captureTimestamp,
        DataResidency dataResidency,
        String sourceDeviceId,
        String missionId,
        Map<String, String> technicalMetadata,
        AuditMetadata audit
) {
    public MediaAsset {
        if (mediaAssetId == null || mediaAssetId.isBlank()) {
            throw new IllegalArgumentException("mediaAssetId is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (assetType == null) {
            throw new IllegalArgumentException("assetType is required");
        }
        if (sourceType == null) {
            throw new IllegalArgumentException("sourceType is required");
        }
        if (storageUri == null || storageUri.isBlank()) {
            throw new IllegalArgumentException("storageUri is required");
        }
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must be zero or greater");
        }
        if (audit == null) {
            throw new IllegalArgumentException("audit is required");
        }
        technicalMetadata = technicalMetadata == null ? Map.of() : Map.copyOf(technicalMetadata);
    }
}

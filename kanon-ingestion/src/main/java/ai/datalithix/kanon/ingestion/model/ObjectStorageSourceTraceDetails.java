package ai.datalithix.kanon.ingestion.model;

public record ObjectStorageSourceTraceDetails(
        String bucket,
        String objectKey,
        String objectVersion,
        String storageUri,
        String checksumSha256,
        String contentType,
        long sizeBytes
) implements SourceTraceDetails {}

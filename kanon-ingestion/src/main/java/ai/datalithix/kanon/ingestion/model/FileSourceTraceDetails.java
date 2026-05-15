package ai.datalithix.kanon.ingestion.model;

public record FileSourceTraceDetails(
        String originalFilename,
        String contentType,
        long sizeBytes,
        String storageUri,
        String checksumSha256
) implements SourceTraceDetails {}

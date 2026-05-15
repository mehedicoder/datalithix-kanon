package ai.datalithix.kanon.ingestion.model;

import java.util.Map;

public record SourcePayload(
        PayloadLocationType locationType,
        String location,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String checksumSha256,
        Map<String, String> metadata
) {}

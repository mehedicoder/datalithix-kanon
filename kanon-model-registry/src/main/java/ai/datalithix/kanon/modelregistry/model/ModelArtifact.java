package ai.datalithix.kanon.modelregistry.model;

public record ModelArtifact(
        String artifactUri,
        String framework,
        long sizeBytes,
        String checksum,
        String storageType
) {
    public ModelArtifact {
        if (artifactUri == null || artifactUri.isBlank()) {
            throw new IllegalArgumentException("artifactUri is required");
        }
    }
}

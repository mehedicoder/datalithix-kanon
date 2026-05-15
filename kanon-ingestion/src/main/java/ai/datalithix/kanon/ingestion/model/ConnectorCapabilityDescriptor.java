package ai.datalithix.kanon.ingestion.model;

import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.SourceType;
import java.util.Set;

public record ConnectorCapabilityDescriptor(
        V1ConnectorType connectorType,
        Set<SourceType> supportedSourceTypes,
        Set<AssetType> supportedAssetTypes,
        boolean supportsPayloadStorage,
        boolean supportsIdempotency,
        boolean supportsCheckpointing,
        boolean supportsBatchImport,
        boolean supportsRetry
) {
    public ConnectorCapabilityDescriptor {
        if (connectorType == null) {
            throw new IllegalArgumentException("connectorType is required");
        }
        supportedSourceTypes = supportedSourceTypes == null ? Set.of() : Set.copyOf(supportedSourceTypes);
        supportedAssetTypes = supportedAssetTypes == null ? Set.of() : Set.copyOf(supportedAssetTypes);
    }
}

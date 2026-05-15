package ai.datalithix.kanon.annotation.model;

import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DomainType;
import java.util.Set;

public record AnnotationNodeDescriptor(
        String nodeId,
        AnnotationExecutionNodeType nodeType,
        String displayName,
        Set<AssetType> supportedAssetTypes,
        Set<DomainType> supportedDomains,
        Set<AnnotationGeometryType> supportedGeometryTypes,
        Set<String> supportedLabelTypes,
        boolean enabled
) {
    public AnnotationNodeDescriptor {
        if (nodeId == null || nodeId.isBlank()) {
            throw new IllegalArgumentException("nodeId is required");
        }
        if (nodeType == null) {
            throw new IllegalArgumentException("nodeType is required");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName is required");
        }
        supportedAssetTypes = supportedAssetTypes == null ? Set.of() : Set.copyOf(supportedAssetTypes);
        supportedDomains = supportedDomains == null ? Set.of() : Set.copyOf(supportedDomains);
        supportedGeometryTypes = supportedGeometryTypes == null ? Set.of() : Set.copyOf(supportedGeometryTypes);
        supportedLabelTypes = supportedLabelTypes == null ? Set.of() : Set.copyOf(supportedLabelTypes);
    }
}

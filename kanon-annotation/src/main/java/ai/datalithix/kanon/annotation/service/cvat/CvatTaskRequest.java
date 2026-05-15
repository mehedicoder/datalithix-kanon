package ai.datalithix.kanon.annotation.service.cvat;

import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DomainType;
import java.util.Map;
import java.util.Set;

public record CvatTaskRequest(
        String kanonTaskId,
        String tenantId,
        String caseId,
        String projectRef,
        AssetType assetType,
        DomainType domainType,
        String mediaAssetId,
        Set<String> labels,
        Map<String, String> mediaRefs,
        Map<String, String> preAnnotations,
        Map<String, String> instructions
) {
    public CvatTaskRequest {
        if (kanonTaskId == null || kanonTaskId.isBlank()) {
            throw new IllegalArgumentException("kanonTaskId is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (assetType == null) {
            throw new IllegalArgumentException("assetType is required");
        }
        labels = labels == null ? Set.of() : Set.copyOf(labels);
        mediaRefs = mediaRefs == null ? Map.of() : Map.copyOf(mediaRefs);
        preAnnotations = preAnnotations == null ? Map.of() : Map.copyOf(preAnnotations);
        instructions = instructions == null ? Map.of() : Map.copyOf(instructions);
    }
}

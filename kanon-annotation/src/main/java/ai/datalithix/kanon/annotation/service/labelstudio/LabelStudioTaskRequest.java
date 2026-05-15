package ai.datalithix.kanon.annotation.service.labelstudio;

import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DomainType;
import java.util.Map;
import java.util.Set;

public record LabelStudioTaskRequest(
        String kanonTaskId,
        String tenantId,
        String caseId,
        String projectRef,
        AssetType assetType,
        DomainType domainType,
        Set<String> labels,
        Map<String, String> data,
        Map<String, String> preAnnotations,
        Map<String, String> instructions
) {
    public LabelStudioTaskRequest {
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
        data = data == null ? Map.of() : Map.copyOf(data);
        preAnnotations = preAnnotations == null ? Map.of() : Map.copyOf(preAnnotations);
        instructions = instructions == null ? Map.of() : Map.copyOf(instructions);
    }
}

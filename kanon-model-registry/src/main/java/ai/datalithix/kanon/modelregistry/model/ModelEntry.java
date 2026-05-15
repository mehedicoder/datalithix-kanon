package ai.datalithix.kanon.modelregistry.model;

import ai.datalithix.kanon.common.model.AuditMetadata;
import java.util.List;
import java.util.Set;

public record ModelEntry(
        String modelEntryId,
        String tenantId,
        String modelName,
        String description,
        String framework,
        String taskType,
        String domainType,
        Set<String> complianceTags,
        int latestVersionNumber,
        String latestLifecycleStage,
        String latestVersionId,
        boolean enabled,
        List<String> versionIds,
        AuditMetadata audit
) {
    public ModelEntry {
        if (modelEntryId == null || modelEntryId.isBlank()) {
            throw new IllegalArgumentException("modelEntryId is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException("modelName is required");
        }
        if (audit == null) {
            throw new IllegalArgumentException("audit is required");
        }
        complianceTags = complianceTags == null ? Set.of() : Set.copyOf(complianceTags);
        versionIds = versionIds == null ? List.of() : List.copyOf(versionIds);
    }
}

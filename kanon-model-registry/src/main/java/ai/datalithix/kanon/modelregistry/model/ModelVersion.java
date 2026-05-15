package ai.datalithix.kanon.modelregistry.model;

import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.training.model.HyperParameterConfig;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public record ModelVersion(
        String modelVersionId,
        String modelEntryId,
        String tenantId,
        int versionNumber,
        String trainingJobId,
        String datasetVersionId,
        String datasetDefinitionId,
        ModelArtifact artifact,
        HyperParameterConfig hyperParameters,
        Set<String> complianceTags,
        ModelLifecycleStage lifecycleStage,
        String promotedBy,
        Instant promotedAt,
        List<String> evaluationRunIds,
        List<String> deploymentTargetIds,
        List<String> evidenceEventIds,
        AuditMetadata audit
) {
    public ModelVersion {
        if (modelVersionId == null || modelVersionId.isBlank()) {
            throw new IllegalArgumentException("modelVersionId is required");
        }
        if (modelEntryId == null || modelEntryId.isBlank()) {
            throw new IllegalArgumentException("modelEntryId is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (artifact == null) {
            throw new IllegalArgumentException("artifact is required");
        }
        if (lifecycleStage == null) {
            throw new IllegalArgumentException("lifecycleStage is required");
        }
        if (audit == null) {
            throw new IllegalArgumentException("audit is required");
        }
        complianceTags = complianceTags == null ? Set.of() : Set.copyOf(complianceTags);
        evaluationRunIds = evaluationRunIds == null ? List.of() : List.copyOf(evaluationRunIds);
        deploymentTargetIds = deploymentTargetIds == null ? List.of() : List.copyOf(deploymentTargetIds);
        evidenceEventIds = evidenceEventIds == null ? List.of() : List.copyOf(evidenceEventIds);
    }
}

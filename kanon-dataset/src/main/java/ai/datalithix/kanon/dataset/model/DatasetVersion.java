package ai.datalithix.kanon.dataset.model;

import ai.datalithix.kanon.common.model.AuditMetadata;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record DatasetVersion(
        String datasetVersionId,
        String datasetDefinitionId,
        String tenantId,
        int versionNumber,
        String curationRuleId,
        SplitStrategy splitStrategy,
        List<DatasetSplit> splits,
        Map<String, Long> labelDistribution,
        Map<String, Double> classBalance,
        long totalRecordCount,
        Instant curatedAt,
        String curatedBy,
        String exportStatus,
        String exportFormat,
        String exportArtifactUri,
        String failureReason,
        List<String> evidenceEventIds,
        AuditMetadata audit
) {
    public DatasetVersion {
        if (datasetVersionId == null || datasetVersionId.isBlank()) {
            throw new IllegalArgumentException("datasetVersionId is required");
        }
        if (datasetDefinitionId == null || datasetDefinitionId.isBlank()) {
            throw new IllegalArgumentException("datasetDefinitionId is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (versionNumber < 1) {
            throw new IllegalArgumentException("versionNumber must be >= 1");
        }
        if (audit == null) {
            throw new IllegalArgumentException("audit is required");
        }
        splits = splits == null ? List.of() : List.copyOf(splits);
        labelDistribution = labelDistribution == null ? Map.of() : Map.copyOf(labelDistribution);
        classBalance = classBalance == null ? Map.of() : Map.copyOf(classBalance);
        evidenceEventIds = evidenceEventIds == null ? List.of() : List.copyOf(evidenceEventIds);
    }
}

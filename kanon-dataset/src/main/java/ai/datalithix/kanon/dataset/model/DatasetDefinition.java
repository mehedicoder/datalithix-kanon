package ai.datalithix.kanon.dataset.model;

import ai.datalithix.kanon.common.model.AuditMetadata;
import java.util.List;
import java.util.Set;

public record DatasetDefinition(
        String datasetDefinitionId,
        String tenantId,
        String name,
        String description,
        String domainType,
        Set<String> sourceAnnotationIds,
        CurationRule curationRule,
        SplitStrategy splitStrategy,
        double trainRatio,
        double valRatio,
        double testRatio,
        List<ExportFormat> exportFormats,
        String dataResidency,
        boolean enabled,
        int latestVersionNumber,
        AuditMetadata audit
) {
    public DatasetDefinition {
        if (datasetDefinitionId == null || datasetDefinitionId.isBlank()) {
            throw new IllegalArgumentException("datasetDefinitionId is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (audit == null) {
            throw new IllegalArgumentException("audit is required");
        }
        sourceAnnotationIds = sourceAnnotationIds == null ? Set.of() : Set.copyOf(sourceAnnotationIds);
        exportFormats = exportFormats == null ? List.of() : List.copyOf(exportFormats);
        if (Math.abs(trainRatio + valRatio + testRatio - 1.0) > 0.001) {
            throw new IllegalArgumentException("train/val/test ratios must sum to 1.0");
        }
    }

    public DatasetDefinition(
            String datasetDefinitionId,
            String tenantId,
            String name,
            String description,
            String domainType,
            double trainRatio,
            double valRatio,
            double testRatio,
            AuditMetadata audit
    ) {
        this(datasetDefinitionId, tenantId, name, description, domainType, Set.of(), null, SplitStrategy.RANDOM,
                trainRatio, valRatio, testRatio, List.of(ExportFormat.JSONL), null, true, 0, audit);
    }
}

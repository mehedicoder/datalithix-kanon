package ai.datalithix.kanon.dataset.model;

import java.util.List;

public record DatasetSplit(
        String splitType,
        double ratio,
        List<String> annotationRecordIds
) {
    public DatasetSplit {
        if (splitType == null || splitType.isBlank()) {
            throw new IllegalArgumentException("splitType is required");
        }
        if (ratio <= 0 || ratio > 1) {
            throw new IllegalArgumentException("ratio must be between 0 and 1");
        }
        annotationRecordIds = annotationRecordIds == null ? List.of() : List.copyOf(annotationRecordIds);
    }
}

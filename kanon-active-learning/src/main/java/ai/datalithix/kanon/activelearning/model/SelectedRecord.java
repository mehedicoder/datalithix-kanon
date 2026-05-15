package ai.datalithix.kanon.activelearning.model;

import java.util.Map;

public record SelectedRecord(
        String recordId,
        double score,
        String selectionReason,
        Map<String, Object> metadata
) {
    public SelectedRecord {
        if (recordId == null || recordId.isBlank()) throw new IllegalArgumentException("recordId is required");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}

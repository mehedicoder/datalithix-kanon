package ai.datalithix.kanon.annotation.service.labelstudio;

import java.util.Map;

public record LabelStudioTaskRef(
        String externalTaskId,
        String externalUrl,
        Map<String, String> metadata
) {
    public LabelStudioTaskRef {
        if (externalTaskId == null || externalTaskId.isBlank()) {
            throw new IllegalArgumentException("externalTaskId is required");
        }
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}

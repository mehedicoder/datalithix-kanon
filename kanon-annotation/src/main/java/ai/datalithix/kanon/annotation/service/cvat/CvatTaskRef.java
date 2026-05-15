package ai.datalithix.kanon.annotation.service.cvat;

import java.util.Map;

public record CvatTaskRef(
        String externalTaskId,
        String externalUrl,
        Map<String, String> metadata
) {
    public CvatTaskRef {
        if (externalTaskId == null || externalTaskId.isBlank()) {
            throw new IllegalArgumentException("externalTaskId is required");
        }
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}

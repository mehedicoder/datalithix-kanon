package ai.datalithix.kanon.annotation.model;

import java.util.Map;

public record AnnotationResultItem(
        String label,
        AnnotationGeometryType geometryType,
        String geometryJson,
        Integer frameStart,
        Integer frameEnd,
        Long startTimeMs,
        Long endTimeMs,
        String textValue,
        String confidence,
        Map<String, String> attributes
) {
    public AnnotationResultItem {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label is required");
        }
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}

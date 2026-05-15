package ai.datalithix.kanon.config.model;

public record FieldDefinition(
        String name,
        String displayName,
        FieldType type,
        boolean required
) {}

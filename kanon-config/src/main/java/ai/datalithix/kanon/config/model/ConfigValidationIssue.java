package ai.datalithix.kanon.config.model;

public record ConfigValidationIssue(
        ConfigValidationSeverity severity,
        String path,
        String message
) {}

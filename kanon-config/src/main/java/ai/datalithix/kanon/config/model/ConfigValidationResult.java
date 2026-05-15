package ai.datalithix.kanon.config.model;

import java.util.List;

public record ConfigValidationResult(
        List<ConfigValidationIssue> issues
) {
    public boolean valid() {
        return issues == null || issues.stream().noneMatch(issue -> issue.severity() == ConfigValidationSeverity.ERROR);
    }

    public static ConfigValidationResult ok() {
        return new ConfigValidationResult(List.of());
    }
}

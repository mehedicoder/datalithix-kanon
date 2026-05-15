package ai.datalithix.kanon.config.model;

public record ModelRouteTemplate(
        String primaryProfile,
        String fallbackProfile,
        String reason
) {}

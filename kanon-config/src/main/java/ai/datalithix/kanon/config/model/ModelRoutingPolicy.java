package ai.datalithix.kanon.config.model;

import ai.datalithix.kanon.common.AiTaskType;
import java.util.Map;

public record ModelRoutingPolicy(
        String id,
        String displayName,
        Map<AiTaskType, ModelRouteTemplate> routes
) {}

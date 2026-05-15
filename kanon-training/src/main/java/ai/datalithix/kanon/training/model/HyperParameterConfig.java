package ai.datalithix.kanon.training.model;

import java.util.Map;

public record HyperParameterConfig(
        String framework,
        String modelArchitecture,
        int epochs,
        int batchSize,
        double learningRate,
        Map<String, String> extraParams
) {
    public HyperParameterConfig {
        if (framework == null || framework.isBlank()) {
            throw new IllegalArgumentException("framework is required");
        }
        extraParams = extraParams == null ? Map.of() : Map.copyOf(extraParams);
    }
}

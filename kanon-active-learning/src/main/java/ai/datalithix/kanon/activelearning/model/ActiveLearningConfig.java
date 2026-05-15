package ai.datalithix.kanon.activelearning.model;

import java.util.Map;

public record ActiveLearningConfig(
        SelectionStrategyType strategyType,
        double minConfidenceThreshold,
        int maxRecordsPerCycle,
        int minNewRecordsForRetraining,
        boolean autoTriggerRetraining,
        String scheduleCron,
        boolean enabled,
        Map<String, String> strategyParams
) {
    public ActiveLearningConfig {
        if (strategyType == null) throw new IllegalArgumentException("strategyType is required");
        if (minConfidenceThreshold < 0 || minConfidenceThreshold > 1)
            throw new IllegalArgumentException("minConfidenceThreshold must be between 0 and 1");
        if (maxRecordsPerCycle < 1) throw new IllegalArgumentException("maxRecordsPerCycle must be >= 1");
        strategyParams = strategyParams == null ? Map.of() : Map.copyOf(strategyParams);
    }
}

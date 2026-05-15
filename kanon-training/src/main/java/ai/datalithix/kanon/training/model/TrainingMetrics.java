package ai.datalithix.kanon.training.model;

import java.time.Instant;
import java.util.Map;

public record TrainingMetrics(
        double loss,
        double accuracy,
        Map<String, Double> extraMetrics,
        int currentEpoch,
        int totalEpochs,
        Instant recordedAt
) {
    public TrainingMetrics {
        extraMetrics = extraMetrics == null ? Map.of() : Map.copyOf(extraMetrics);
    }
}

package ai.datalithix.kanon.modelregistry.model;

public record EvaluationMetric(
        String metricName,
        double value,
        String description
) {
    public EvaluationMetric {
        if (metricName == null || metricName.isBlank()) {
            throw new IllegalArgumentException("metricName is required");
        }
    }
}

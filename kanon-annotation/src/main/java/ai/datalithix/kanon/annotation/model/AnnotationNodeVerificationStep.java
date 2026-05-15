package ai.datalithix.kanon.annotation.model;

public record AnnotationNodeVerificationStep(
        String stepName,
        AnnotationNodeVerificationStepStatus status,
        String detail,
        long latencyMs
) {
}

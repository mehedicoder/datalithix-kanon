package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.AnnotationNodeVerificationStep;

public record ProviderVerificationResult(
        AnnotationNodeVerificationStep authenticationStep,
        String version
) {
}

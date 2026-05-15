package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.ExternalAnnotationNode;
import ai.datalithix.kanon.annotation.model.ExternalAnnotationProviderType;

public interface AnnotationProviderVerificationClient {
    ExternalAnnotationProviderType providerType();

    ProviderVerificationResult verify(ExternalAnnotationNode node, String resolvedSecret);
}

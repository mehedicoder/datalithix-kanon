package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.AnnotationNodeVerificationResult;
import ai.datalithix.kanon.annotation.model.ExternalAnnotationNode;

public interface AnnotationNodeVerificationService {
    AnnotationNodeVerificationResult verify(ExternalAnnotationNode node);
}

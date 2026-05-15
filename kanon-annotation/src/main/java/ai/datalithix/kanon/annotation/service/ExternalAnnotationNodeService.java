package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.AnnotationNodeVerificationResult;
import ai.datalithix.kanon.annotation.model.ExternalAnnotationNode;
import ai.datalithix.kanon.annotation.model.ExternalAnnotationProviderType;
import java.util.List;
import java.util.Optional;

public interface ExternalAnnotationNodeService {
    List<ExternalAnnotationNode> list(String requesterTenantId, boolean platformScoped);

    Optional<ExternalAnnotationNode> findById(String requesterTenantId, String nodeId, boolean platformScoped);

    ExternalAnnotationNode create(
            String requesterTenantId,
            boolean platformScoped,
            String tenantId,
            String displayName,
            ExternalAnnotationProviderType providerType,
            String baseUrl,
            String secretRef,
            String storageBucket,
            String actorId
    );

    ExternalAnnotationNode update(
            String requesterTenantId,
            boolean platformScoped,
            String nodeId,
            String displayName,
            String baseUrl,
            String secretRef,
            String storageBucket,
            boolean enabled,
            String actorId
    );

    AnnotationNodeVerificationResult testConnection(String requesterTenantId, boolean platformScoped, String nodeId, String actorId);

    void delete(String requesterTenantId, boolean platformScoped, String nodeId, String actorId);
}

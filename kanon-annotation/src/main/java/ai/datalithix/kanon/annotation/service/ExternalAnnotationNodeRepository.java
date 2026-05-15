package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.ExternalAnnotationNode;
import ai.datalithix.kanon.annotation.model.ExternalAnnotationProviderType;
import ai.datalithix.kanon.common.service.PagedQueryPort;
import java.util.List;
import java.util.Optional;

public interface ExternalAnnotationNodeRepository extends PagedQueryPort<ExternalAnnotationNode> {
    ExternalAnnotationNode save(ExternalAnnotationNode node);

    Optional<ExternalAnnotationNode> findById(String tenantId, String nodeId);

    List<ExternalAnnotationNode> findByTenant(String tenantId);

    List<ExternalAnnotationNode> findByTenantAndProvider(String tenantId, ExternalAnnotationProviderType providerType);

    void deleteById(String tenantId, String nodeId);
}

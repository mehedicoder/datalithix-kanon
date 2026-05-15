package ai.datalithix.kanon.ingestion.service;

import ai.datalithix.kanon.common.service.PagedQueryPort;
import ai.datalithix.kanon.ingestion.model.SourceDescriptor;
import java.util.Optional;

public interface SourceDescriptorRepository extends PagedQueryPort<SourceDescriptor> {
    SourceDescriptor save(String tenantId, SourceDescriptor sourceDescriptor);

    Optional<SourceDescriptor> findBySourceIdentity(
            String tenantId,
            String sourceSystem,
            String sourceIdentifier
    );
}

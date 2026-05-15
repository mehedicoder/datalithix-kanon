package ai.datalithix.kanon.ingestion.service;

import ai.datalithix.kanon.common.service.PagedQueryPort;
import ai.datalithix.kanon.ingestion.model.SourceTrace;
import java.util.List;
import java.util.Optional;

public interface SourceTraceRepository extends PagedQueryPort<SourceTrace> {
    SourceTrace save(SourceTrace sourceTrace);

    Optional<SourceTrace> findById(String tenantId, String sourceTraceId);

    Optional<SourceTrace> findByCorrelationId(String tenantId, String correlationId);

    Optional<SourceTrace> findBySourceIdentity(
            String tenantId,
            String sourceSystem,
            String sourceIdentifier
    );

    List<SourceTrace> findByCaseId(String tenantId, String caseId);
}

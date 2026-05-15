package ai.datalithix.kanon.ingestion.service;

import ai.datalithix.kanon.common.service.PagedQueryPort;
import ai.datalithix.kanon.ingestion.model.IngestionBatch;
import java.util.Optional;

public interface IngestionBatchRepository extends PagedQueryPort<IngestionBatch> {
    IngestionBatch save(IngestionBatch ingestionBatch);

    Optional<IngestionBatch> findById(String tenantId, String importBatchId);
}

package ai.datalithix.kanon.ingestion.service;

import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.ingestion.model.IngestionBatch;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryIngestionBatchRepository implements IngestionBatchRepository {
    private final Map<String, IngestionBatch> batches = new ConcurrentHashMap<>();

    @Override
    public IngestionBatch save(IngestionBatch batch) {
        batches.put(batch.tenantId() + ":" + batch.importBatchId(), batch);
        return batch;
    }

    @Override
    public Optional<IngestionBatch> findById(String tenantId, String importBatchId) {
        return Optional.ofNullable(batches.get(tenantId + ":" + importBatchId));
    }

    @Override
    public PageResult<IngestionBatch> findPage(QuerySpec query) {
        var matching = batches.values().stream()
                .filter(b -> b.tenantId().equals(query.tenantId()))
                .sorted(Comparator.comparing(IngestionBatch::startedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        int limit = query.page().pageSize();
        int offset = query.page().pageNumber() * limit;
        var items = offset >= matching.size() ? List.<IngestionBatch>of()
                : matching.subList(offset, Math.min(offset + limit, matching.size()));
        return new PageResult<>(items, query.page().pageNumber(), query.page().pageSize(), matching.size());
    }
}

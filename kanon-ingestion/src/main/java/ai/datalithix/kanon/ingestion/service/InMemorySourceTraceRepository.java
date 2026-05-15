package ai.datalithix.kanon.ingestion.service;

import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.ingestion.model.SourceTrace;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySourceTraceRepository implements SourceTraceRepository {
    private final Map<String, SourceTrace> traces = new ConcurrentHashMap<>();

    @Override
    public SourceTrace save(SourceTrace trace) {
        traces.put(trace.tenantId() + ":" + trace.sourceTraceId(), trace);
        return trace;
    }

    @Override
    public Optional<SourceTrace> findById(String tenantId, String sourceTraceId) {
        return Optional.ofNullable(traces.get(tenantId + ":" + sourceTraceId));
    }

    @Override
    public Optional<SourceTrace> findByCorrelationId(String tenantId, String correlationId) {
        return traces.values().stream()
                .filter(t -> t.tenantId().equals(tenantId) && correlationId.equals(t.correlationId()))
                .findFirst();
    }

    @Override
    public Optional<SourceTrace> findBySourceIdentity(String tenantId, String sourceSystem, String sourceIdentifier) {
        return traces.values().stream()
                .filter(t -> t.tenantId().equals(tenantId))
                .filter(t -> t.source().sourceSystem().equals(sourceSystem))
                .filter(t -> t.source().sourceIdentifier().equals(sourceIdentifier))
                .max(Comparator.comparing(SourceTrace::ingestionTimestamp));
    }

    @Override
    public List<SourceTrace> findByCaseId(String tenantId, String caseId) {
        return traces.values().stream()
                .filter(t -> t.tenantId().equals(tenantId) && caseId.equals(t.caseId()))
                .sorted(Comparator.comparing(SourceTrace::ingestionTimestamp).reversed())
                .toList();
    }

    @Override
    public PageResult<SourceTrace> findPage(QuerySpec query) {
        var matching = traces.values().stream()
                .filter(t -> t.tenantId().equals(query.tenantId()))
                .sorted(Comparator.comparing(SourceTrace::ingestionTimestamp).reversed())
                .toList();
        int limit = query.page().pageSize();
        int offset = query.page().pageNumber() * limit;
        var items = offset >= matching.size() ? List.<SourceTrace>of()
                : matching.subList(offset, Math.min(offset + limit, matching.size()));
        return new PageResult<>(items, query.page().pageNumber(), query.page().pageSize(), matching.size());
    }
}

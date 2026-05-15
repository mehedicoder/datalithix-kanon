package ai.datalithix.kanon.ingestion.service;

import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.ingestion.model.ConnectorHealth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryConnectorHealthRepository implements ConnectorHealthRepository {
    private final Map<String, ConnectorHealth> healthMap = new ConcurrentHashMap<>();

    @Override
    public ConnectorHealth save(String tenantId, ConnectorHealth health) {
        healthMap.put(tenantId + ":" + health.connectorId(), health);
        return health;
    }

    @Override
    public Optional<ConnectorHealth> findByConnectorId(String tenantId, String connectorId) {
        return Optional.ofNullable(healthMap.get(tenantId + ":" + connectorId));
    }

    @Override
    public PageResult<ConnectorHealth> findPage(QuerySpec query) {
        var matching = healthMap.values().stream()
                .filter(h -> h.connectorId() != null)
                .filter(h -> {
                    var entry = healthMap.entrySet().stream()
                            .filter(e -> e.getValue().connectorId().equals(h.connectorId()))
                            .findFirst();
                    return entry.isPresent() && entry.get().getKey().startsWith(query.tenantId() + ":");
                })
                .sorted(Comparator.comparing(ConnectorHealth::lastIngestionAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        int limit = query.page().pageSize();
        int offset = query.page().pageNumber() * limit;
        var items = offset >= matching.size() ? List.<ConnectorHealth>of()
                : matching.subList(offset, Math.min(offset + limit, matching.size()));
        return new PageResult<>(items, query.page().pageNumber(), query.page().pageSize(), matching.size());
    }
}

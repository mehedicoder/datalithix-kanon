package ai.datalithix.kanon.ingestion.service;

import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.ingestion.model.ConnectorConfiguration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryConnectorConfigurationRepository implements ConnectorConfigurationRepository {
    private final Map<String, ConnectorConfiguration> configs = new ConcurrentHashMap<>();

    @Override
    public ConnectorConfiguration save(ConnectorConfiguration config) {
        configs.put(config.tenantId() + ":" + config.connectorId(), config);
        return config;
    }

    @Override
    public Optional<ConnectorConfiguration> findById(String tenantId, String connectorId) {
        return Optional.ofNullable(configs.get(tenantId + ":" + connectorId));
    }

    @Override
    public PageResult<ConnectorConfiguration> findPage(QuerySpec query) {
        var matching = configs.values().stream()
                .filter(c -> c.tenantId().equals(query.tenantId()))
                .sorted(Comparator.comparing(ConnectorConfiguration::sourceCategory)
                        .thenComparing(ConnectorConfiguration::sourceType))
                .toList();
        int limit = query.page().pageSize();
        int offset = query.page().pageNumber() * limit;
        var items = offset >= matching.size() ? List.<ConnectorConfiguration>of()
                : matching.subList(offset, Math.min(offset + limit, matching.size()));
        return new PageResult<>(items, query.page().pageNumber(), query.page().pageSize(), matching.size());
    }
}

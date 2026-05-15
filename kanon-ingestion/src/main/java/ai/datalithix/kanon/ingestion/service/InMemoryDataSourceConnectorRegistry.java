package ai.datalithix.kanon.ingestion.service;

import ai.datalithix.kanon.common.SourceCategory;
import ai.datalithix.kanon.common.SourceType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class InMemoryDataSourceConnectorRegistry implements DataSourceConnectorRegistry {
    private final List<DataSourceConnector> connectors;

    public InMemoryDataSourceConnectorRegistry(List<DataSourceConnector> connectors) {
        this.connectors = List.copyOf(connectors);
    }

    @Override
    public List<DataSourceConnector> connectors() {
        return connectors;
    }

    @Override
    public Optional<DataSourceConnector> findById(String connectorId) {
        return connectors.stream()
                .filter(connector -> connector.connectorId().equals(connectorId))
                .findFirst();
    }

    @Override
    public Optional<DataSourceConnector> findByType(SourceCategory sourceCategory, SourceType sourceType) {
        return connectors.stream()
                .filter(connector -> connector.sourceCategory() == sourceCategory)
                .filter(connector -> connector.sourceType() == sourceType
                        || connector.connectorType().equals(ai.datalithix.kanon.ingestion.model.V1ConnectorType.from(sourceCategory, sourceType).orElse(null)))
                .findFirst();
    }
}

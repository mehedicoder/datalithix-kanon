package ai.datalithix.kanon.ingestion.service;

import ai.datalithix.kanon.common.SourceCategory;
import ai.datalithix.kanon.common.SourceType;
import java.util.List;
import java.util.Optional;

public interface DataSourceConnectorRegistry {
    List<DataSourceConnector> connectors();

    Optional<DataSourceConnector> findById(String connectorId);

    Optional<DataSourceConnector> findByType(SourceCategory sourceCategory, SourceType sourceType);
}

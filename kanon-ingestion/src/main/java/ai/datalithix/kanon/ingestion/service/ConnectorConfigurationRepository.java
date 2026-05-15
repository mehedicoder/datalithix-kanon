package ai.datalithix.kanon.ingestion.service;

import ai.datalithix.kanon.common.service.PagedQueryPort;
import ai.datalithix.kanon.ingestion.model.ConnectorConfiguration;
import java.util.Optional;

public interface ConnectorConfigurationRepository extends PagedQueryPort<ConnectorConfiguration> {
    ConnectorConfiguration save(ConnectorConfiguration connectorConfiguration);

    Optional<ConnectorConfiguration> findById(String tenantId, String connectorId);
}

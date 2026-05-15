package ai.datalithix.kanon.ingestion.service;

import ai.datalithix.kanon.common.service.PagedQueryPort;
import ai.datalithix.kanon.ingestion.model.ConnectorHealth;
import java.util.Optional;

public interface ConnectorHealthRepository extends PagedQueryPort<ConnectorHealth> {
    ConnectorHealth save(String tenantId, ConnectorHealth connectorHealth);

    Optional<ConnectorHealth> findByConnectorId(String tenantId, String connectorId);
}

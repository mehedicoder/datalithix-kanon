package ai.datalithix.kanon.api.source;

import ai.datalithix.kanon.common.SourceCategory;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.ingestion.model.ConnectorCapabilityDescriptor;
import ai.datalithix.kanon.ingestion.model.ConnectorExecutionPolicy;
import ai.datalithix.kanon.ingestion.model.ConnectorHealth;
import ai.datalithix.kanon.ingestion.model.SourceTraceabilityRequirement;
import ai.datalithix.kanon.ingestion.model.V1ConnectorContracts;
import ai.datalithix.kanon.ingestion.model.V1ConnectorType;
import ai.datalithix.kanon.ingestion.service.DataSourceConnector;
import ai.datalithix.kanon.ingestion.service.DataSourceConnectorRegistry;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/sources/connectors")
public class SourceConnectorReadController {
    private final DataSourceConnectorRegistry connectorRegistry;

    public SourceConnectorReadController(DataSourceConnectorRegistry connectorRegistry) {
        this.connectorRegistry = connectorRegistry;
    }

    @GetMapping
    public List<ConnectorSummary> connectors(@RequestParam(required = false) SourceCategory sourceCategory,
                                             @RequestParam(required = false) SourceType sourceType) {
        return connectorRegistry.connectors().stream()
                .filter(connector -> sourceCategory == null || connector.sourceCategory() == sourceCategory)
                .filter(connector -> sourceType == null || connector.sourceType() == sourceType)
                .map(ConnectorSummary::from)
                .toList();
    }

    @GetMapping("/{connectorId}")
    public ConnectorSummary connector(@PathVariable String connectorId) {
        return connectorRegistry.findById(connectorId)
                .map(ConnectorSummary::from)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Connector not found: " + connectorId));
    }

    @GetMapping("/capabilities")
    public List<ConnectorCapabilityDescriptor> capabilities() {
        return V1ConnectorContracts.CAPABILITIES;
    }

    @GetMapping("/traceability-requirements")
    public List<SourceTraceabilityRequirement> traceabilityRequirements() {
        return V1ConnectorContracts.TRACEABILITY_REQUIREMENTS;
    }

    public record ConnectorSummary(
            String connectorId,
            SourceCategory sourceCategory,
            SourceType sourceType,
            V1ConnectorType connectorType,
            ConnectorHealth health,
            ConnectorExecutionPolicy executionPolicy
    ) {
        static ConnectorSummary from(DataSourceConnector connector) {
            return new ConnectorSummary(
                    connector.connectorId(),
                    connector.sourceCategory(),
                    connector.sourceType(),
                    connector.connectorType(),
                    connector.health(),
                    connector.executionPolicy()
            );
        }
    }
}

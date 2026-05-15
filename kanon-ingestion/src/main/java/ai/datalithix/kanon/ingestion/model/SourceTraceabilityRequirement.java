package ai.datalithix.kanon.ingestion.model;

import java.util.Set;

public record SourceTraceabilityRequirement(
        V1ConnectorType connectorType,
        Set<String> mandatoryFields,
        Set<String> connectorSpecificFields
) {
    public SourceTraceabilityRequirement {
        if (connectorType == null) {
            throw new IllegalArgumentException("connectorType is required");
        }
        mandatoryFields = mandatoryFields == null ? Set.of() : Set.copyOf(mandatoryFields);
        connectorSpecificFields = connectorSpecificFields == null ? Set.of() : Set.copyOf(connectorSpecificFields);
    }
}

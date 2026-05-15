package ai.datalithix.kanon.ingestion.service;

import ai.datalithix.kanon.common.SourceCategory;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.ingestion.model.ConnectorExecutionPolicy;
import ai.datalithix.kanon.ingestion.model.ConnectorHealth;
import ai.datalithix.kanon.ingestion.model.V1ConnectorType;
import ai.datalithix.kanon.ingestion.model.IngestionRequest;
import ai.datalithix.kanon.ingestion.model.IngestionResult;

public interface DataSourceConnector {
    String connectorId();

    SourceCategory sourceCategory();

    SourceType sourceType();

    default V1ConnectorType connectorType() {
        return V1ConnectorType.from(sourceCategory(), sourceType()).orElse(V1ConnectorType.CUSTOM);
    }

    ConnectorHealth health();

    ConnectorExecutionPolicy executionPolicy();

    boolean supports(IngestionRequest request);

    IngestionResult ingest(IngestionRequest request);
}

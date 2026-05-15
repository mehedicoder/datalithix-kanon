package ai.datalithix.kanon.ingestion.service;

import ai.datalithix.kanon.common.SourceCategory;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.ingestion.model.EnterpriseRecordSourceTraceDetails;
import ai.datalithix.kanon.ingestion.model.IngestionRequest;
import ai.datalithix.kanon.ingestion.model.SourcePayload;
import ai.datalithix.kanon.ingestion.model.SourceTraceDetails;
import ai.datalithix.kanon.ingestion.model.V1ConnectorType;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "kanon.connectors.database-import-enabled", havingValue = "true", matchIfMissing = true)
public class PostgresDatabaseImportConnector extends AbstractV1DataSourceConnector {
    public PostgresDatabaseImportConnector() {
        super("v1-postgres-database-import", V1ConnectorType.DATABASE_IMPORT, SourceCategory.STORAGE, SourceType.DATABASE_IMPORT,
                Set.of(SourceType.DATABASE_IMPORT), defaultPolicy(false, true, true));
    }

    @Override
    protected SourceTraceDetails details(IngestionRequest request) {
        return new EnterpriseRecordSourceTraceDetails(
                attribute(request, "connector_name"),
                attribute(request, "external_record_id"),
                attribute(request, "external_record_version"),
                attribute(request, "query_ref"),
                attribute(request, "import_batch_id")
        );
    }
}

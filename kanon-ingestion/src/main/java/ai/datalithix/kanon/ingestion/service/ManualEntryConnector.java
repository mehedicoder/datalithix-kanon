package ai.datalithix.kanon.ingestion.service;

import ai.datalithix.kanon.common.SourceCategory;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.ingestion.model.IngestionRequest;
import ai.datalithix.kanon.ingestion.model.ManualEntrySourceTraceDetails;
import ai.datalithix.kanon.ingestion.model.SourceTraceDetails;
import ai.datalithix.kanon.ingestion.model.V1ConnectorType;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "kanon.connectors.manual-entry-enabled", havingValue = "true", matchIfMissing = true)
public class ManualEntryConnector extends AbstractV1DataSourceConnector {
    public ManualEntryConnector() {
        super("v1-manual-entry", V1ConnectorType.MANUAL_ENTRY, SourceCategory.INTERACTIVE, SourceType.MANUAL_ENTRY,
                Set.of(SourceType.MANUAL_ENTRY), defaultPolicy(false, false, false));
    }

    @Override
    protected SourceTraceDetails details(IngestionRequest request) {
        return new ManualEntrySourceTraceDetails(
                attribute(request, "form_id"),
                attribute(request, "note_id"),
                attribute(request, "review_task_id"),
                attribute(request, "correction_reason")
        );
    }
}

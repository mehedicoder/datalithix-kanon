package ai.datalithix.kanon.ingestion.service;

import ai.datalithix.kanon.common.SourceCategory;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.ingestion.model.FileSourceTraceDetails;
import ai.datalithix.kanon.ingestion.model.IngestionRequest;
import ai.datalithix.kanon.ingestion.model.SourcePayload;
import ai.datalithix.kanon.ingestion.model.SourceTraceDetails;
import ai.datalithix.kanon.ingestion.model.V1ConnectorType;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "kanon.connectors.upload-enabled", havingValue = "true", matchIfMissing = true)
public class UploadConnector extends AbstractV1DataSourceConnector {
    public UploadConnector() {
        super("v1-upload", V1ConnectorType.UPLOAD, SourceCategory.DOCUMENT, SourceType.FILE_UPLOAD,
                Set.of(SourceType.FILE_UPLOAD), defaultPolicy(true, false, true));
    }

    @Override
    protected SourceTraceDetails details(IngestionRequest request) {
        SourcePayload payload = request.payload();
        return new FileSourceTraceDetails(
                payload == null ? null : payload.originalFilename(),
                payload == null ? null : payload.contentType(),
                payload == null ? 0 : payload.sizeBytes(),
                payload == null ? null : payload.location(),
                payload == null ? null : payload.checksumSha256()
        );
    }
}

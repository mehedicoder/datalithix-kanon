package ai.datalithix.kanon.ingestion.service;

import ai.datalithix.kanon.common.SourceCategory;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.ingestion.model.IngestionRequest;
import ai.datalithix.kanon.ingestion.model.ObjectStorageSourceTraceDetails;
import ai.datalithix.kanon.ingestion.model.SourcePayload;
import ai.datalithix.kanon.ingestion.model.SourceTraceDetails;
import ai.datalithix.kanon.ingestion.model.V1ConnectorType;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "kanon.connectors.object-storage-enabled", havingValue = "true", matchIfMissing = true)
public class ObjectStorageConnector extends AbstractV1DataSourceConnector {
    public ObjectStorageConnector() {
        super("v1-object-storage", V1ConnectorType.OBJECT_STORAGE, SourceCategory.STORAGE, SourceType.OBJECT_STORAGE,
                Set.of(SourceType.OBJECT_STORAGE), defaultPolicy(true, true, true));
    }

    @Override
    protected SourceTraceDetails details(IngestionRequest request) {
        SourcePayload payload = request.payload();
        return new ObjectStorageSourceTraceDetails(
                attribute(request, "bucket"),
                attribute(request, "object_key"),
                attribute(request, "object_version"),
                payload == null ? request.source().sourceUri() : payload.location(),
                payload == null ? null : payload.checksumSha256(),
                payload == null ? null : payload.contentType(),
                payload == null ? 0 : payload.sizeBytes()
        );
    }
}

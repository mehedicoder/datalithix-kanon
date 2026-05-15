package ai.datalithix.kanon.ingestion.service;

import ai.datalithix.kanon.common.SourceCategory;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.ingestion.model.ApiWebhookSourceTraceDetails;
import ai.datalithix.kanon.ingestion.model.IngestionRequest;
import ai.datalithix.kanon.ingestion.model.SourcePayload;
import ai.datalithix.kanon.ingestion.model.SourceTraceDetails;
import ai.datalithix.kanon.ingestion.model.V1ConnectorType;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "kanon.connectors.rest-webhook-enabled", havingValue = "true", matchIfMissing = true)
public class RestWebhookConnector extends AbstractV1DataSourceConnector {
    public RestWebhookConnector() {
        super("v1-rest-webhook", V1ConnectorType.REST_WEBHOOK, SourceCategory.API, SourceType.REST_API,
                Set.of(SourceType.REST_API, SourceType.WEBHOOK), defaultPolicy(true, false, true));
    }

    @Override
    protected SourceTraceDetails details(IngestionRequest request) {
        return new ApiWebhookSourceTraceDetails(
                attribute(request, "http_method"),
                attribute(request, "endpoint"),
                attribute(request, "external_request_id"),
                request.idempotencyKey(),
                attribute(request, "callback_url"),
                intAttribute(request, "response_status")
        );
    }
}

package ai.datalithix.kanon.ingestion.model;

public record ApiWebhookSourceTraceDetails(
        String httpMethod,
        String endpoint,
        String externalRequestId,
        String idempotencyKey,
        String callbackUrl,
        int responseStatus
) implements SourceTraceDetails {}

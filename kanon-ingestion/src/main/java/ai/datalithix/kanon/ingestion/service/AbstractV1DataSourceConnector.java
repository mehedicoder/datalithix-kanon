package ai.datalithix.kanon.ingestion.service;

import ai.datalithix.kanon.common.ActorType;
import ai.datalithix.kanon.common.SourceCategory;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.common.runtime.BackpressurePolicy;
import ai.datalithix.kanon.common.runtime.BackpressureStrategy;
import ai.datalithix.kanon.common.runtime.ExecutionControls;
import ai.datalithix.kanon.common.runtime.PayloadTransferPolicy;
import ai.datalithix.kanon.common.runtime.RetryPolicy;
import ai.datalithix.kanon.ingestion.model.ConnectorExecutionPolicy;
import ai.datalithix.kanon.ingestion.model.ConnectorHealth;
import ai.datalithix.kanon.ingestion.model.ConnectorHealthStatus;
import ai.datalithix.kanon.ingestion.model.IngestionRequest;
import ai.datalithix.kanon.ingestion.model.IngestionResult;
import ai.datalithix.kanon.ingestion.model.IngestionStatus;
import ai.datalithix.kanon.ingestion.model.SourcePayload;
import ai.datalithix.kanon.ingestion.model.SourceTrace;
import ai.datalithix.kanon.ingestion.model.SourceTraceDetails;
import ai.datalithix.kanon.ingestion.model.V1ConnectorType;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public abstract class AbstractV1DataSourceConnector implements DataSourceConnector {
    private final String connectorId;
    private final V1ConnectorType connectorType;
    private final SourceCategory sourceCategory;
    private final SourceType sourceType;
    private final Set<SourceType> supportedSourceTypes;
    private final ConnectorExecutionPolicy executionPolicy;

    protected AbstractV1DataSourceConnector(
            String connectorId,
            V1ConnectorType connectorType,
            SourceCategory sourceCategory,
            SourceType sourceType,
            Set<SourceType> supportedSourceTypes,
            ConnectorExecutionPolicy executionPolicy
    ) {
        this.connectorId = connectorId;
        this.connectorType = connectorType;
        this.sourceCategory = sourceCategory;
        this.sourceType = sourceType;
        this.supportedSourceTypes = Set.copyOf(supportedSourceTypes);
        this.executionPolicy = executionPolicy;
    }

    @Override
    public String connectorId() {
        return connectorId;
    }

    @Override
    public V1ConnectorType connectorType() {
        return connectorType;
    }

    @Override
    public SourceCategory sourceCategory() {
        return sourceCategory;
    }

    @Override
    public SourceType sourceType() {
        return sourceType;
    }

    @Override
    public ConnectorHealth health() {
        return new ConnectorHealth(connectorId, ConnectorHealthStatus.HEALTHY, null, null, null, null, 0, 0);
    }

    @Override
    public ConnectorExecutionPolicy executionPolicy() {
        return executionPolicy;
    }

    @Override
    public boolean supports(IngestionRequest request) {
        return request != null
                && request.source() != null
                && request.source().sourceCategory() == sourceCategory
                && supportedSourceTypes.contains(request.source().sourceType());
    }

    @Override
    public IngestionResult ingest(IngestionRequest request) {
        Instant completedAt = Instant.now();
        if (!supports(request)) {
            return new IngestionResult(
                    request == null ? null : request.requestId(),
                    request == null ? null : request.tenantId(),
                    IngestionStatus.REJECTED,
                    null,
                    List.of(),
                    null,
                    "Connector does not support the request source category/type",
                    completedAt
            );
        }
        String requestId = valueOrGenerated(request.requestId(), "request");
        String sourceTraceId = "trace-" + requestId;
        String evidenceEventId = "evidence-" + requestId;
        SourceTrace sourceTrace = new SourceTrace(
                sourceTraceId,
                request.tenantId(),
                request.caseId(),
                request.source(),
                originalPayloadHash(request.payload()),
                actorType(request),
                actorId(request),
                request.requestedAt() == null ? completedAt : request.requestedAt(),
                request.correlationId(),
                evidenceEventId,
                details(request),
                new AuditMetadata(completedAt, actorId(request), completedAt, actorId(request), 1)
        );
        return new IngestionResult(
                requestId,
                request.tenantId(),
                IngestionStatus.COMPLETED,
                sourceTrace,
                List.of(),
                evidenceEventId,
                null,
                completedAt
        );
    }

    protected abstract SourceTraceDetails details(IngestionRequest request);

    protected String attribute(IngestionRequest request, String key) {
        return request.attributes() == null ? null : request.attributes().get(key);
    }

    protected int intAttribute(IngestionRequest request, String key) {
        String value = attribute(request, key);
        return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
    }

    protected static ConnectorExecutionPolicy defaultPolicy(boolean objectStorageRequired, boolean checkpointingRequired, boolean batchImport) {
        return new ConnectorExecutionPolicy(
                new ExecutionControls(Duration.ofSeconds(30), 3, 4, 120),
                new RetryPolicy(3, Duration.ofMillis(250), Duration.ofSeconds(5)),
                new BackpressurePolicy(1_000, BackpressureStrategy.DEFER_TO_QUEUE),
                new PayloadTransferPolicy(65_536, objectStorageRequired, objectStorageRequired, true),
                batchImport ? 100 : 1,
                true,
                checkpointingRequired,
                true
        );
    }

    private static String originalPayloadHash(SourcePayload payload) {
        return payload == null ? null : payload.checksumSha256();
    }

    private static ActorType actorType(IngestionRequest request) {
        return request.accessContext() == null ? ActorType.SYSTEM : ActorType.HUMAN;
    }

    private static String actorId(IngestionRequest request) {
        return request.accessContext() == null ? "system" : request.accessContext().userId();
    }

    private static String valueOrGenerated(String value, String prefix) {
        return value == null || value.isBlank() ? prefix + "-" + Instant.now().toEpochMilli() : value;
    }
}

package ai.datalithix.kanon.ingestion.service;

import ai.datalithix.kanon.common.ActorType;
import ai.datalithix.kanon.common.security.SecurityAuditEvent;
import ai.datalithix.kanon.common.security.SecurityEventType;
import ai.datalithix.kanon.evidence.model.EvidenceEvent;
import ai.datalithix.kanon.evidence.service.EvidenceLedger;
import ai.datalithix.kanon.ingestion.model.ConnectorHealth;
import ai.datalithix.kanon.ingestion.model.ConnectorHealthStatus;
import ai.datalithix.kanon.ingestion.model.IngestionRequest;
import ai.datalithix.kanon.ingestion.model.IngestionResult;
import ai.datalithix.kanon.ingestion.model.IngestionStatus;
import ai.datalithix.kanon.policy.security.SecurityAuditEventPublisher;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class IngestionOrchestrationService {
    private final DataSourceConnectorRegistry connectorRegistry;
    private final SourceTraceRepository sourceTraceRepository;
    private final SourceDescriptorRepository sourceDescriptorRepository;
    private final ConnectorHealthRepository connectorHealthRepository;
    private final EvidenceLedger evidenceLedger;
    private final SecurityAuditEventPublisher auditPublisher;

    public IngestionOrchestrationService(
            DataSourceConnectorRegistry connectorRegistry,
            SourceTraceRepository sourceTraceRepository,
            SourceDescriptorRepository sourceDescriptorRepository,
            ConnectorHealthRepository connectorHealthRepository,
            EvidenceLedger evidenceLedger,
            SecurityAuditEventPublisher auditPublisher
    ) {
        this.connectorRegistry = connectorRegistry;
        this.sourceTraceRepository = sourceTraceRepository;
        this.sourceDescriptorRepository = sourceDescriptorRepository;
        this.connectorHealthRepository = connectorHealthRepository;
        this.evidenceLedger = evidenceLedger;
        this.auditPublisher = auditPublisher;
    }

    public IngestionResult ingest(IngestionRequest request) {
        Instant startedAt = Instant.now();
        var connector = connectorRegistry.findByType(
                request.source().sourceCategory(), request.source().sourceType())
                .orElse(null);
        if (connector == null) {
            return recordFailure(request, startedAt, "No connector found for "
                    + request.source().sourceCategory() + "/" + request.source().sourceType());
        }

        sourceDescriptorRepository.save(request.tenantId(), request.source());

        IngestionResult result;
        try {
            result = connector.ingest(request);
        } catch (Exception e) {
            return recordFailure(request, startedAt, "Ingestion failed: " + e.getMessage());
        }

        String realEventId = UUID.randomUUID().toString();
        String eventType = switch (result.status()) {
            case COMPLETED, STORED, ACCEPTED -> "SOURCE_INGESTED";
            case DUPLICATE -> "SOURCE_DUPLICATE";
            case REJECTED -> "SOURCE_REJECTED";
            case FAILED -> "SOURCE_FAILED";
        };

        var beforeState = new HashMap<String, Object>();
        beforeState.put("sourceSystem", request.source().sourceSystem());
        beforeState.put("sourceIdentifier", request.source().sourceIdentifier());
        beforeState.put("connectorId", connector.connectorId());
        var afterState = new HashMap<String, Object>();
        afterState.put("status", result.status().name());
        afterState.put("requestId", result.requestId());

        var evidenceEvent = new EvidenceEvent(
                realEventId,
                request.tenantId(),
                request.caseId(),
                eventType,
                ActorType.HUMAN,
                actorId(request),
                null, null, null,
                beforeState,
                afterState,
                result.failureReason(),
                Instant.now()
        );
        evidenceLedger.append(evidenceEvent);

        if (result.sourceTrace() != null) {
            var traceWithRealEventId = new ai.datalithix.kanon.ingestion.model.SourceTrace(
                    result.sourceTrace().sourceTraceId(),
                    result.sourceTrace().tenantId(),
                    result.sourceTrace().caseId(),
                    result.sourceTrace().source(),
                    result.sourceTrace().originalPayloadHash(),
                    result.sourceTrace().actorType(),
                    result.sourceTrace().actorId(),
                    result.sourceTrace().ingestionTimestamp(),
                    result.sourceTrace().correlationId(),
                    realEventId,
                    result.sourceTrace().details(),
                    result.sourceTrace().audit()
            );
            sourceTraceRepository.save(traceWithRealEventId);
        }

        updateHealth(request, connector.connectorId(), result, null);

        var auditAttrs = new HashMap<String, String>();
        auditAttrs.put("connectorId", connector.connectorId());
        auditAttrs.put("sourceSystem", request.source().sourceSystem());
        auditAttrs.put("evidenceEventId", realEventId);
        auditPublisher.publish(new SecurityAuditEvent(
                UUID.randomUUID().toString(),
                request.tenantId(),
                actorId(request),
                result.status() == IngestionStatus.FAILED || result.status() == IngestionStatus.REJECTED
                        ? SecurityEventType.INGESTION_FAILED
                        : SecurityEventType.INGESTION_PERFORMED,
                null,
                result.status().name(),
                result.failureReason(),
                Instant.now(),
                nonNullAttrs(auditAttrs)
        ));

        return new IngestionResult(
                result.requestId(),
                result.tenantId(),
                result.status(),
                result.sourceTrace(),
                result.createdAssetIds(),
                realEventId,
                result.failureReason(),
                result.completedAt()
        );
    }

    private IngestionResult recordFailure(IngestionRequest request, Instant startedAt, String reason) {
        updateHealth(request, request.connectorId(), null, reason);
        var auditAttrs = new HashMap<String, String>();
        auditAttrs.put("connectorId", request.connectorId());
        if (request.source() != null) {
            auditAttrs.put("sourceSystem", request.source().sourceSystem());
        }
        auditPublisher.publish(new SecurityAuditEvent(
                UUID.randomUUID().toString(), request.tenantId(), actorId(request),
                SecurityEventType.INGESTION_FAILED, null, "FAILED", reason, Instant.now(),
                nonNullAttrs(auditAttrs)
        ));
        return new IngestionResult(
                request.requestId(), request.tenantId(), IngestionStatus.FAILED,
                null, java.util.List.of(), null, reason, Instant.now()
        );
    }

    private void updateHealth(IngestionRequest request, String connectorId, IngestionResult result, String failureReason) {
        Instant now = Instant.now();
        ConnectorHealth health = new ConnectorHealth(
                connectorId != null ? connectorId : request.connectorId(),
                result != null && result.status() == IngestionStatus.COMPLETED
                        ? ConnectorHealthStatus.HEALTHY : ConnectorHealthStatus.DEGRADED,
                now,
                result != null && result.status() == IngestionStatus.COMPLETED ? now : null,
                result == null || result.status() == IngestionStatus.FAILED ? now : null,
                failureReason != null ? failureReason : result != null ? result.failureReason() : null,
                result != null && result.status() == IngestionStatus.FAILED ? 1 : 0,
                0
        );
        connectorHealthRepository.save(request.tenantId(), health);
    }

    private static String actorId(IngestionRequest request) {
        return request.accessContext() == null ? "system" : request.accessContext().userId();
    }

    private static Map<String, String> nonNullAttrs(Map<String, String> map) {
        return map.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}

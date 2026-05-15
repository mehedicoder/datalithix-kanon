package ai.datalithix.kanon.ingestion.service;

import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.ActorType;
import ai.datalithix.kanon.common.DataResidency;
import ai.datalithix.kanon.common.SourceCategory;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.common.compliance.ComplianceClassification;
import ai.datalithix.kanon.common.compliance.DataClassification;
import ai.datalithix.kanon.common.security.SecurityAuditEvent;
import ai.datalithix.kanon.common.security.SecurityEventType;
import ai.datalithix.kanon.evidence.model.EvidenceEvent;
import ai.datalithix.kanon.evidence.service.EvidenceLedger;
import ai.datalithix.kanon.ingestion.model.ConnectorHealth;
import ai.datalithix.kanon.ingestion.model.IngestionRequest;
import ai.datalithix.kanon.ingestion.model.IngestionResult;
import ai.datalithix.kanon.ingestion.model.IngestionStatus;
import ai.datalithix.kanon.ingestion.model.PayloadLocationType;
import ai.datalithix.kanon.ingestion.model.SourceDescriptor;
import ai.datalithix.kanon.ingestion.model.SourcePayload;
import ai.datalithix.kanon.ingestion.model.SourceTrace;
import ai.datalithix.kanon.policy.security.SecurityAuditEventPublisher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IngestionOrchestrationServiceTest {
    private InMemorySourceTraceRepository traceRepo;
    private InMemorySourceDescriptorRepository descriptorRepo;
    private InMemoryConnectorHealthRepository healthRepo;
    private CapturingEvidenceLedger ledger;
    private CapturingAuditPublisher auditPublisher;
    private IngestionOrchestrationService service;

    @BeforeEach
    void setUp() {
        traceRepo = new InMemorySourceTraceRepository();
        descriptorRepo = new InMemorySourceDescriptorRepository();
        healthRepo = new InMemoryConnectorHealthRepository();
        ledger = new CapturingEvidenceLedger();
        auditPublisher = new CapturingAuditPublisher();
        var registry = new InMemoryDataSourceConnectorRegistry(
                List.of(new UploadConnector(), new ManualEntryConnector()));
        service = new IngestionOrchestrationService(
                registry, traceRepo, descriptorRepo, healthRepo, ledger, auditPublisher);
    }

    @Test
    void ingestsSourceDataThroughConnector() {
        var request = createRequest(SourceCategory.DOCUMENT, SourceType.FILE_UPLOAD, "sys-1", "file-1");
        var result = service.ingest(request);

        assertEquals(IngestionStatus.COMPLETED, result.status());
        assertNotNull(result.sourceTrace());
        assertNotNull(result.evidenceEventId());
    }

    @Test
    void savesSourceDescriptor() {
        var request = createRequest(SourceCategory.DOCUMENT, SourceType.FILE_UPLOAD, "sys-1", "file-2");
        service.ingest(request);

        var found = descriptorRepo.findBySourceIdentity("tenant-1", "sys-1", "file-2");
        assertTrue(found.isPresent());
        assertEquals(SourceCategory.DOCUMENT, found.get().sourceCategory());
    }

    @Test
    void savesSourceTrace() {
        var request = createRequest(SourceCategory.DOCUMENT, SourceType.FILE_UPLOAD, "sys-1", "file-3");
        var result = service.ingest(request);

        var found = traceRepo.findById("tenant-1", result.sourceTrace().sourceTraceId());
        assertTrue(found.isPresent());
    }

    @Test
    void appendsEvidenceEvent() {
        var request = createRequest(SourceCategory.DOCUMENT, SourceType.FILE_UPLOAD, "sys-1", "file-4");
        service.ingest(request);

        assertFalse(ledger.events.isEmpty());
        var event = (EvidenceEvent) ledger.events.get(0);
        assertEquals("tenant-1", event.tenantId());
        assertTrue(event.eventType().contains("SOURCE_"));
    }

    @Test
    void publishesAuditEvent() {
        var request = createRequest(SourceCategory.DOCUMENT, SourceType.FILE_UPLOAD, "sys-1", "file-5");
        service.ingest(request);

        assertFalse(auditPublisher.events.isEmpty());
        var event = auditPublisher.events.get(0);
        assertEquals("tenant-1", event.tenantId());
        assertEquals(SecurityEventType.INGESTION_PERFORMED, event.eventType());
    }

    @Test
    void returnsFailedResultWhenNoConnectorFound() {
        var request = createRequest(SourceCategory.COMMUNICATION, SourceType.EMAIL_INBOX, "sys-2", "email-1");
        var result = service.ingest(request);

        assertEquals(IngestionStatus.FAILED, result.status());
        assertTrue(result.failureReason().contains("No connector found"));
    }

    @Test
    void updatesConnectorHealthOnSuccess() {
        var request = createRequest(SourceCategory.DOCUMENT, SourceType.FILE_UPLOAD, "sys-1", "file-6");
        service.ingest(request);

        var health = healthRepo.findByConnectorId("tenant-1", "v1-upload");
        assertTrue(health.isPresent());
        assertNotNull(health.get().lastIngestionAt());
        assertNotNull(health.get().lastSuccessAt());
    }

    @Test
    void updatesConnectorHealthOnFailure() {
        var descriptor = new SourceDescriptor(
                SourceCategory.COMMUNICATION, SourceType.EMAIL_INBOX, "sys-3", "email-fail", null,
                null, null, null, null, null, null);
        var badRequest = new IngestionRequest(
                "req-1", "tenant-1", "v1-upload", descriptor, null, null, null, null,
                java.time.Instant.now(), Map.of(), null);
        var result = service.ingest(badRequest);

        assertEquals(IngestionStatus.FAILED, result.status());
        var health = healthRepo.findByConnectorId("tenant-1", "v1-upload");
        assertTrue(health.isPresent());
    }

    @Test
    void publishesFailureAuditEventOnConnectorError() {
        var descriptor = new SourceDescriptor(
                SourceCategory.COMMUNICATION, SourceType.EMAIL_INBOX, "sys-4", "email-fail-2", null,
                null, null, null, null, null, null);
        var request = new IngestionRequest(
                "req-2", "tenant-1", "v1-upload", descriptor, null, null, null, null,
                java.time.Instant.now(), Map.of(), null);
        service.ingest(request);

        var hasFailureEvent = auditPublisher.events.stream()
                .anyMatch(e -> e.eventType() == SecurityEventType.INGESTION_FAILED);
        assertTrue(hasFailureEvent);
    }

    @Test
    void evidenceEventHasSourceMetadata() {
        var request = createRequest(SourceCategory.DOCUMENT, SourceType.FILE_UPLOAD, "sys-5", "file-7");
        service.ingest(request);

        var event = (EvidenceEvent) ledger.events.get(0);
        assertNotNull(event.beforeState());
        assertEquals("sys-5", event.beforeState().get("sourceSystem"));
        assertEquals("file-7", event.beforeState().get("sourceIdentifier"));
    }

    private static IngestionRequest createRequest(SourceCategory category, SourceType type,
                                                   String sourceSystem, String sourceIdentifier) {
        var descriptor = new SourceDescriptor(
                category, type, sourceSystem, sourceIdentifier, "uri://test",
                AssetType.DOCUMENT, DataClassification.INTERNAL, ComplianceClassification.NONE,
                DataResidency.EU, "retain-7y", null);
        var payload = new SourcePayload(PayloadLocationType.INLINE, null, "test.txt",
                "text/plain", 100, "abc123", Map.of());
        return new IngestionRequest(
                null, "tenant-1", null, descriptor, payload, null, "case-1", null,
                java.time.Instant.now(), Map.of(), null);
    }

    private static class CapturingEvidenceLedger implements EvidenceLedger {
        final List<Object> events = new ArrayList<>();
        @Override public void append(EvidenceEvent event) { events.add(event); }
    }

    private static class CapturingAuditPublisher implements SecurityAuditEventPublisher {
        final List<SecurityAuditEvent> events = new ArrayList<>();
        @Override public void publish(SecurityAuditEvent event) { events.add(event); }
    }
}

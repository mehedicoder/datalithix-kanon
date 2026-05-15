package ai.datalithix.kanon.api.intake;

import ai.datalithix.kanon.common.storage.ObjectStorageClient;
import ai.datalithix.kanon.common.storage.ObjectStorageObject;
import ai.datalithix.kanon.common.storage.ObjectStoragePutRequest;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class IntakeControllerTest {
    private final StubStorageClient storage = new StubStorageClient();
    private final IntakeController controller = new IntakeController(storage);

    @Test
    void createSessionReturnsSessionWithId() {
        var request = new IntakeController.CreateSessionRequest("CASE_A", "Test case", Map.of("key", "val"));
        var session = controller.createSession("tenant-1", request);
        assertNotNull(session.sessionId());
        assertEquals("tenant-1", session.tenantId());
        assertEquals("CASE_A", session.caseType());
        assertTrue(session.uploads().isEmpty());
    }

    @Test
    void uploadFilesAddsToSession() throws Exception {
        var session = controller.createSession("tenant-1",
                new IntakeController.CreateSessionRequest("CASE_A", "desc", Map.of()));
        var file = new MockMultipartFile("files", "doc.pdf", "application/pdf", new byte[]{1, 2, 3, 4});
        var updated = controller.uploadFiles(session.sessionId(), List.of(file));
        assertEquals(1, updated.uploads().size());
        assertEquals("doc.pdf", updated.uploads().getFirst().originalFilename());
        assertEquals(4, updated.uploads().getFirst().sizeBytes());
    }

    @Test
    void uploadFilesThrowsOnNonexistentSession() {
        var file = new MockMultipartFile("files", "doc.pdf", "application/pdf", new byte[]{1});
        assertThrows(IllegalArgumentException.class,
                () -> controller.uploadFiles("nonexistent", List.of(file)));
    }

    @Test
    void dispatchRemovesSessionAndReturnsResult() {
        var session = controller.createSession("tenant-1",
                new IntakeController.CreateSessionRequest("CASE_A", "desc", Map.of()));
        var result = controller.dispatch(session.sessionId(), "tenant-1");
        assertEquals(session.sessionId(), result.sessionId());
        assertEquals("DISPATCHED", result.status());
        assertEquals(0, result.fileCount());
    }

    @Test
    void dispatchThrowsOnNonexistentSession() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.dispatch("nonexistent", "tenant-1"));
    }

    @Test
    void listSessionsReturnsTenantSessions() {
        controller.createSession("tenant-1",
                new IntakeController.CreateSessionRequest("CASE_A", "desc", Map.of()));
        controller.createSession("tenant-2",
                new IntakeController.CreateSessionRequest("CASE_B", "other", Map.of()));
        var sessions = controller.listSessions("tenant-1");
        assertEquals(1, sessions.size());
        assertEquals("CASE_A", sessions.getFirst().caseType());
    }

    @Test
    void getSessionReturnsSession() {
        var created = controller.createSession("tenant-1",
                new IntakeController.CreateSessionRequest("CASE_A", "desc", Map.of()));
        var found = controller.getSession(created.sessionId());
        assertNotNull(found);
        assertEquals(created.sessionId(), found.sessionId());
    }

    @Test
    void getSessionThrowsOnNonexistent() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.getSession("nonexistent"));
    }

    private static class StubStorageClient implements ObjectStorageClient {
        @Override
        public ObjectStorageObject put(ObjectStoragePutRequest request) {
            return new ObjectStorageObject(request.tenantId(), "bucket", request.objectKey(),
                    "s3://bucket/" + request.objectKey(), request.checksumSha256(),
                    request.contentType(), request.sizeBytes(), Instant.now(), Map.of());
        }

        @Override
        public ObjectStorageObject metadata(String tenantId, String objectKey) { return null; }

        @Override
        public URI presignedReadUrl(String tenantId, String objectKey, Duration ttl) {
            return URI.create("http://minio/read/" + objectKey);
        }

        @Override
        public URI presignedWriteUrl(String tenantId, String objectKey, Duration ttl) {
            return URI.create("http://minio/write/" + objectKey);
        }

        @Override
        public void deleteMarker(String tenantId, String objectKey) {}

        @Override
        public boolean verifyChecksum(String tenantId, String objectKey, String checksumSha256) {
            return true;
        }
    }
}

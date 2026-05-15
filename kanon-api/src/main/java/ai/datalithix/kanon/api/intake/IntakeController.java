package ai.datalithix.kanon.api.intake;

import ai.datalithix.kanon.common.storage.ObjectStorageClient;
import ai.datalithix.kanon.common.storage.ObjectStoragePutRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/intake")
@Tag(name = "Intake Workspace", description = "Create cases and tasks with bulk file upload before dispatch")
public class IntakeController {

    private final ObjectStorageClient storageClient;

    private final java.util.concurrent.ConcurrentHashMap<String, IntakeSession> sessions = new java.util.concurrent.ConcurrentHashMap<>();

    public IntakeController(ObjectStorageClient storageClient) {
        this.storageClient = storageClient;
    }

    @PostMapping("/sessions")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER"})
    @Operation(summary = "Create an intake session", description = "Creates a new intake session for bulk upload and case creation")
    public IntakeSession createSession(
            @Parameter(description = "Tenant ID") @RequestParam(defaultValue = "demo-tenant") String tenantId,
            @RequestBody CreateSessionRequest request
    ) {
        var sessionId = "is-" + UUID.randomUUID();
        var session = new IntakeSession(sessionId, tenantId, request.caseType(),
                request.description(), request.attributes(), new ArrayList<>(), Instant.now());
        sessions.put(sessionId, session);
        return session;
    }

    @PostMapping(value = "/sessions/{sessionId}/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER"})
    @Operation(summary = "Upload file to intake session", description = "Uploads one or more files to an intake session")
    public IntakeSession uploadFiles(
            @Parameter(description = "Session ID") @PathVariable String sessionId,
            @Parameter(description = "Files to upload") @RequestParam("files") List<MultipartFile> files
    ) throws IOException {
        var session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        for (var file : files) {
            var objectKey = session.sessionId + "/" + UUID.randomUUID() + "/" + file.getOriginalFilename();
            var request = new ObjectStoragePutRequest(
                    session.tenantId, objectKey, file.getInputStream(),
                    file.getContentType(), file.getSize(), null, Map.of(
                    "originalFilename", file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown",
                    "sessionId", sessionId
            ));
            var result = storageClient.put(request);
            session.uploads().add(new UploadedFile(result.objectKey(), result.storageUri(),
                    file.getOriginalFilename(), file.getSize(), file.getContentType()));
        }
        return session;
    }

    @PostMapping("/sessions/{sessionId}/dispatch")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER"})
    @Operation(summary = "Dispatch intake session", description = "Finalizes and dispatches the intake session for processing")
    public DispatchResult dispatch(
            @Parameter(description = "Session ID") @PathVariable String sessionId,
            @Parameter(description = "Tenant ID") @RequestParam(defaultValue = "demo-tenant") String tenantId
    ) {
        var session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        sessions.remove(sessionId);
        return new DispatchResult(sessionId, "DISPATCHED", session.uploads().size(),
                "Intake session dispatched with " + session.uploads().size() + " files", Instant.now());
    }

    @GetMapping("/sessions")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER"})
    @Operation(summary = "List active intake sessions")
    public List<IntakeSession> listSessions(
            @Parameter(description = "Tenant ID") @RequestParam(defaultValue = "demo-tenant") String tenantId
    ) {
        return sessions.values().stream()
                .filter(s -> s.tenantId().equals(tenantId))
                .toList();
    }

    @GetMapping("/sessions/{sessionId}")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER"})
    @Operation(summary = "Get intake session details")
    public IntakeSession getSession(@PathVariable String sessionId) {
        var session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        return session;
    }

    public record CreateSessionRequest(
            String caseType, String description, Map<String, String> attributes) {}

    public record IntakeSession(
            String sessionId, String tenantId, String caseType, String description,
            Map<String, String> attributes, List<UploadedFile> uploads, Instant createdAt) {}

    public record UploadedFile(
            String objectKey, String storageUri, String originalFilename,
            long sizeBytes, String contentType) {}

    public record DispatchResult(
            String sessionId, String status, int fileCount, String message, Instant dispatchedAt) {}
}

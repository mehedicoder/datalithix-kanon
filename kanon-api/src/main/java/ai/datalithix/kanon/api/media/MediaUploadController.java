package ai.datalithix.kanon.api.media;

import ai.datalithix.kanon.common.storage.ObjectStorageClient;
import ai.datalithix.kanon.common.storage.ObjectStorageObject;
import ai.datalithix.kanon.common.storage.ObjectStoragePutRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
@Tag(name = "Media Upload", description = "Upload and manage media assets via S3-compatible object storage")
public class MediaUploadController {

    private final ObjectStorageClient storageClient;

    public MediaUploadController(ObjectStorageClient storageClient) {
        this.storageClient = storageClient;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER"})
    @Operation(summary = "Upload a file", description = "Uploads a single file to S3-compatible object storage")
    public UploadResponse upload(
            @Parameter(description = "Tenant ID") @RequestParam(defaultValue = "demo-tenant") String tenantId,
            @Parameter(description = "File to upload") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Optional object key (auto-generated if absent)") @RequestParam(required = false) String objectKey,
            @Parameter(description = "Custom metadata (JSON)") @RequestParam(required = false) String metadata
    ) throws IOException {
        var key = objectKey != null ? objectKey : UUID.randomUUID() + "/" + file.getOriginalFilename();
        var checksum = computeSha256(file.getBytes());
        var parsedMetadata = metadata != null ? parseMetadata(metadata) : Map.of(
                "originalFilename", file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown"
        );
        var request = new ObjectStoragePutRequest(
                tenantId, key, file.getInputStream(), file.getContentType(),
                file.getSize(), checksum, parsedMetadata
        );
        var result = storageClient.put(request);
        return new UploadResponse(result.objectKey(), result.storageUri(), result.checksumSha256(), result.sizeBytes());
    }

    @PostMapping("/presigned-url")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER"})
    @Operation(summary = "Generate presigned upload URL",
            description = "Generates a presigned URL for direct client-side upload to S3-compatible storage")
    public PresignedUrlResponse presignedUploadUrl(
            @Parameter(description = "Tenant ID") @RequestParam(defaultValue = "demo-tenant") String tenantId,
            @Parameter(description = "Object key") @RequestParam String objectKey,
            @Parameter(description = "TTL in seconds") @RequestParam(defaultValue = "3600") int ttlSeconds
    ) {
        var url = storageClient.presignedWriteUrl(tenantId, objectKey, Duration.ofSeconds(ttlSeconds));
        return new PresignedUrlResponse(url.toString(), objectKey, ttlSeconds);
    }

    private static String computeSha256(byte[] data) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> parseMetadata(String metadata) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(metadata, Map.class);
        } catch (Exception e) {
            return Map.of("_raw", metadata);
        }
    }

    public record UploadResponse(String objectKey, String storageUri, String checksumSha256, long sizeBytes) {}
    public record PresignedUrlResponse(String url, String objectKey, int ttlSeconds) {}
}

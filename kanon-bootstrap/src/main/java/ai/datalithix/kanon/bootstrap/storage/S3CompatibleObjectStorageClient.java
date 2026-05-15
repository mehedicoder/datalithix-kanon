package ai.datalithix.kanon.bootstrap.storage;

import ai.datalithix.kanon.common.storage.ObjectStorageClient;
import ai.datalithix.kanon.common.storage.ObjectStorageObject;
import ai.datalithix.kanon.common.storage.ObjectStoragePutRequest;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3CompatibleObjectStorageClient implements ObjectStorageClient {
    private static final Logger log = LoggerFactory.getLogger(S3CompatibleObjectStorageClient.class);

    private final MinioClient minioClient;
    private final String bucket;
    private final String tenantPrefixTemplate;

    public S3CompatibleObjectStorageClient(ObjectStorageConfigurationProperties props) {
        var builder = MinioClient.builder()
                .endpoint(props.getEndpoint())
                .credentials(props.getAccessKey(), props.getSecretKey());
        if (props.getRegion() != null && !props.getRegion().isBlank()) {
            builder.region(props.getRegion());
        }
        this.minioClient = builder.build();
        this.bucket = props.getBucket();
        this.tenantPrefixTemplate = props.getTenantPrefixTemplate();
        ensureBucketExists();
    }

    private void ensureBucketExists() {
        try {
            var found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created bucket: {}", bucket);
            }
        } catch (Exception e) {
            log.warn("Could not verify/create bucket '{}': {}", bucket, e.getMessage());
        }
    }

    private String resolveObjectKey(String tenantId, String objectKey) {
        return tenantPrefixTemplate.replace("{tenantId}", tenantId) + "/" + objectKey;
    }

    @Override
    public ObjectStorageObject put(ObjectStoragePutRequest request) {
        var fullKey = resolveObjectKey(request.tenantId(), request.objectKey());
        var contentType = request.contentType() != null ? request.contentType() : "application/octet-stream";
        try {
            var headers = request.metadata() != null
                    ? request.metadata().entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            e -> "X-Amz-Meta-" + e.getKey(),
                            Map.Entry::getValue))
                    : Map.<String, String>of();

            var args = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(fullKey)
                    .stream(request.content(), request.sizeBytes(), -1)
                    .contentType(contentType)
                    .headers(headers)
                    .build();
            minioClient.putObject(args);

            var storageUri = URI.create(bucket + "/" + fullKey);
            return new ObjectStorageObject(
                    request.tenantId(),
                    bucket,
                    request.objectKey(),
                    storageUri.toString(),
                    request.checksumSha256(),
                    contentType,
                    request.sizeBytes(),
                    Instant.now(),
                    request.metadata() != null ? request.metadata() : Map.of()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to put object '" + fullKey + "' in bucket '" + bucket + "'", e);
        }
    }

    @Override
    public ObjectStorageObject metadata(String tenantId, String objectKey) {
        var fullKey = resolveObjectKey(tenantId, objectKey);
        try {
            var stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(fullKey)
                    .build());
            return mapStatToObject(tenantId, objectKey, stat);
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return null;
            }
            throw new RuntimeException("Failed to get metadata for '" + fullKey + "'", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get metadata for '" + fullKey + "'", e);
        }
    }

    @Override
    public URI presignedReadUrl(String tenantId, String objectKey, Duration ttl) {
        return generatePresignedUrl(tenantId, objectKey, ttl, io.minio.http.Method.GET);
    }

    @Override
    public URI presignedWriteUrl(String tenantId, String objectKey, Duration ttl) {
        return generatePresignedUrl(tenantId, objectKey, ttl, io.minio.http.Method.PUT);
    }

    private URI generatePresignedUrl(String tenantId, String objectKey, Duration ttl,
                                       io.minio.http.Method method) {
        var fullKey = resolveObjectKey(tenantId, objectKey);
        try {
            var url = minioClient.getPresignedObjectUrl(
                    io.minio.GetPresignedObjectUrlArgs.builder()
                            .bucket(bucket)
                            .object(fullKey)
                            .method(method)
                            .expiry(Math.toIntExact(ttl.toSeconds()))
                            .build()
            );
            return URI.create(url);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned URL for '" + fullKey + "'", e);
        }
    }

    @Override
    public void deleteMarker(String tenantId, String objectKey) {
        var fullKey = resolveObjectKey(tenantId, objectKey);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(fullKey)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete marker for '" + fullKey + "'", e);
        }
    }

    @Override
    public boolean verifyChecksum(String tenantId, String objectKey, String checksumSha256) {
        if (checksumSha256 == null || checksumSha256.isBlank()) {
            return true;
        }
        var fullKey = resolveObjectKey(tenantId, objectKey);
        try (var response = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(fullKey)
                .build())) {
            var digest = MessageDigest.getInstance("SHA-256");
            var buffer = new byte[8192];
            int read;
            while ((read = response.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            var actual = HexFormat.of().formatHex(digest.digest());
            return actual.equals(checksumSha256);
        } catch (NoSuchAlgorithmException e) {
            log.warn("SHA-256 not available, skipping checksum verification");
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify checksum for '" + fullKey + "'", e);
        }
    }

    private ObjectStorageObject mapStatToObject(String tenantId, String objectKey, StatObjectResponse stat) {
        var userMetadata = stat.userMetadata() != null ? stat.userMetadata() : Map.<String, String>of();
        return new ObjectStorageObject(
                tenantId,
                bucket,
                objectKey,
                URI.create(bucket + "/" + resolveObjectKey(tenantId, objectKey)).toString(),
                null,
                stat.contentType(),
                stat.size(),
                stat.lastModified().toInstant(),
                userMetadata
        );
    }
}

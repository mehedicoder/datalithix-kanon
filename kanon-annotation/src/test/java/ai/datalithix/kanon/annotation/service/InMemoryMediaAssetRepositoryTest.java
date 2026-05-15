package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.MediaAsset;
import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DataResidency;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.PageSpec;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.common.model.SortDirection;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryMediaAssetRepositoryTest {
    private final MediaAssetRepository repo = new InMemoryMediaAssetRepository();

    @Test
    void saveAndFindById() {
        var asset = asset("asset-1", "case-1");
        repo.save(asset);
        var found = repo.findById("tenant-a", "asset-1");
        assertTrue(found.isPresent());
        assertEquals("asset-1", found.get().mediaAssetId());
    }

    @Test
    void findByCaseId() {
        repo.save(asset("a1", "case-1"));
        repo.save(asset("a2", "case-1"));
        repo.save(asset("a3", "case-2"));
        assertEquals(2, repo.findByCaseId("tenant-a", "case-1").size());
    }

    @Test
    void findBySourceTraceId() {
        repo.save(assetWithTrace("a1", "trace-1"));
        repo.save(assetWithTrace("a2", "trace-1"));
        assertEquals(2, repo.findBySourceTraceId("tenant-a", "trace-1").size());
    }

    @Test
    void findPage() {
        repo.save(asset("a1", "case-1"));
        repo.save(asset("a2", "case-2"));
        PageResult<MediaAsset> page = repo.findPage(
                new QuerySpec("tenant-a", new PageSpec(0, 10, null, SortDirection.ASC), null, null));
        assertEquals(2, page.totalItems());
    }

    @Test
    void findByIdReturnsEmptyForUnknown() {
        assertTrue(repo.findById("tenant-a", "unknown").isEmpty());
    }

    private static MediaAsset asset(String id, String caseId) {
        return new MediaAsset(
                id, "tenant-a", caseId, AssetType.VIDEO, SourceType.FILE_UPLOAD, null,
                "s3://bucket/" + id, "abc123", "video/mp4", 1024L,
                60_000L, 30.0, 1920, 1080, Instant.parse("2026-04-17T00:00:00Z"),
                DataResidency.US, "device-1", "mission-1", Map.of("codec", "h264"),
                new AuditMetadata(Instant.now(), "tester", Instant.now(), "tester", 1L));
    }

    private static MediaAsset assetWithTrace(String id, String traceId) {
        return new MediaAsset(
                id, "tenant-a", "case-x", AssetType.IMAGE, SourceType.FILE_UPLOAD, traceId,
                "s3://bucket/" + id, null, "image/jpeg", 512L,
                null, null, null, null, null,
                null, null, null, Map.of(),
                new AuditMetadata(Instant.now(), "tester", Instant.now(), "tester", 1L));
    }
}

package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.AnnotationGeometryType;
import ai.datalithix.kanon.annotation.model.VideoAnnotation;
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

class InMemoryVideoAnnotationRepositoryTest {
    private final VideoAnnotationRepository repo = new InMemoryVideoAnnotationRepository();

    @Test
    void saveAndFindById() {
        var ann = annotation("ann-1", "asset-1", "case-1");
        repo.save(ann);
        var found = repo.findById("tenant-a", "ann-1");
        assertTrue(found.isPresent());
        assertEquals("ann-1", found.get().annotationId());
    }

    @Test
    void findByMediaAssetId() {
        repo.save(annotation("a1", "asset-1", "case-1"));
        repo.save(annotation("a2", "asset-1", "case-1"));
        repo.save(annotation("a3", "asset-2", "case-2"));
        assertEquals(2, repo.findByMediaAssetId("tenant-a", "asset-1").size());
    }

    @Test
    void findByCaseId() {
        repo.save(annotation("a1", "asset-1", "case-1"));
        repo.save(annotation("a2", "asset-2", "case-1"));
        assertEquals(2, repo.findByCaseId("tenant-a", "case-1").size());
    }

    @Test
    void findPage() {
        repo.save(annotation("a1", "asset-1", "case-1"));
        repo.save(annotation("a2", "asset-2", "case-2"));
        PageResult<VideoAnnotation> page = repo.findPage(
                new QuerySpec("tenant-a", new PageSpec(0, 10, null, SortDirection.ASC), null, null));
        assertEquals(2, page.totalItems());
    }

    @Test
    void findByIdReturnsEmptyForUnknown() {
        assertTrue(repo.findById("tenant-a", "unknown").isEmpty());
    }

    private static VideoAnnotation annotation(String id, String assetId, String caseId) {
        return new VideoAnnotation(
                id, "tenant-a", caseId, assetId,
                1, 30, 0L, 1000L,
                AnnotationGeometryType.BOUNDING_BOX,
                "{\"x\":10,\"y\":20,\"w\":30,\"h\":40}",
                "tractor", "track-1", "telemetry-1",
                "approved", "inv-1", "evt-1",
                Map.of("confidence", "0.95"),
                new AuditMetadata(Instant.now(), "tester", Instant.now(), "tester", 1L));
    }
}

package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.VideoAnnotation;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryVideoAnnotationRepository implements VideoAnnotationRepository {
    private final ConcurrentMap<String, VideoAnnotation> store = new ConcurrentHashMap<>();

    @Override
    public VideoAnnotation save(VideoAnnotation annotation) {
        store.put(key(annotation.tenantId(), annotation.annotationId()), annotation);
        return annotation;
    }

    @Override
    public Optional<VideoAnnotation> findById(String tenantId, String annotationId) {
        if (tenantId == null || tenantId.isBlank() || annotationId == null || annotationId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(key(tenantId, annotationId)));
    }

    @Override
    public List<VideoAnnotation> findByMediaAssetId(String tenantId, String mediaAssetId) {
        if (tenantId == null || tenantId.isBlank()) return List.of();
        return store.values().stream()
                .filter(a -> a.tenantId().equals(tenantId) && a.mediaAssetId().equals(mediaAssetId))
                .sorted(Comparator.comparing(VideoAnnotation::annotationId))
                .toList();
    }

    @Override
    public List<VideoAnnotation> findByCaseId(String tenantId, String caseId) {
        if (tenantId == null || tenantId.isBlank()) return List.of();
        return store.values().stream()
                .filter(a -> a.tenantId().equals(tenantId) && a.caseId() != null && a.caseId().equals(caseId))
                .sorted(Comparator.comparing(VideoAnnotation::annotationId))
                .toList();
    }

    @Override
    public PageResult<VideoAnnotation> findPage(QuerySpec query) {
        var all = store.values().stream()
                .sorted(Comparator.comparing(a -> a.audit().updatedAt(), Comparator.reverseOrder()))
                .toList();
        return new PageResult<>(all, 0, all.size(), all.size());
    }

    private static String key(String tenantId, String annotationId) {
        return tenantId + "::" + annotationId;
    }
}

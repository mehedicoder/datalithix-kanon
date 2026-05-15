package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.MediaAsset;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryMediaAssetRepository implements MediaAssetRepository {
    private final ConcurrentMap<String, MediaAsset> store = new ConcurrentHashMap<>();

    @Override
    public MediaAsset save(MediaAsset mediaAsset) {
        store.put(key(mediaAsset.tenantId(), mediaAsset.mediaAssetId()), mediaAsset);
        return mediaAsset;
    }

    @Override
    public Optional<MediaAsset> findById(String tenantId, String mediaAssetId) {
        if (tenantId == null || tenantId.isBlank() || mediaAssetId == null || mediaAssetId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(key(tenantId, mediaAssetId)));
    }

    @Override
    public List<MediaAsset> findByCaseId(String tenantId, String caseId) {
        if (tenantId == null || tenantId.isBlank()) return List.of();
        return store.values().stream()
                .filter(a -> a.tenantId().equals(tenantId) && a.caseId() != null && a.caseId().equals(caseId))
                .sorted(Comparator.comparing(MediaAsset::mediaAssetId))
                .toList();
    }

    @Override
    public List<MediaAsset> findBySourceTraceId(String tenantId, String sourceTraceId) {
        if (tenantId == null || tenantId.isBlank()) return List.of();
        return store.values().stream()
                .filter(a -> a.tenantId().equals(tenantId) && a.sourceTraceId() != null && a.sourceTraceId().equals(sourceTraceId))
                .sorted(Comparator.comparing(MediaAsset::mediaAssetId))
                .toList();
    }

    @Override
    public PageResult<MediaAsset> findPage(QuerySpec query) {
        var all = store.values().stream()
                .sorted(Comparator.comparing(a -> a.audit().updatedAt(), Comparator.reverseOrder()))
                .toList();
        return new PageResult<>(all, 0, all.size(), all.size());
    }

    private static String key(String tenantId, String mediaAssetId) {
        return tenantId + "::" + mediaAssetId;
    }
}

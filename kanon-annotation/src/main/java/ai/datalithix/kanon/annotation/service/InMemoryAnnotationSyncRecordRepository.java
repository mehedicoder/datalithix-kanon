package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.AnnotationSyncRecord;
import ai.datalithix.kanon.annotation.model.AnnotationTaskStatus;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAnnotationSyncRecordRepository implements AnnotationSyncRecordRepository {
    private final Map<String, AnnotationSyncRecord> records = new ConcurrentHashMap<>();

    private static String key(String tenantId, String syncId) {
        return tenantId + ":" + syncId;
    }

    @Override
    public String save(String tenantId, AnnotationSyncRecord record) {
        String syncId = UUID.randomUUID().toString();
        records.put(key(tenantId, syncId), record);
        return syncId;
    }

    @Override
    public Optional<AnnotationSyncRecord> findById(String tenantId, String syncId) {
        return Optional.ofNullable(records.get(key(tenantId, syncId)));
    }

    @Override
    public List<AnnotationSyncRecord> findByAnnotationTaskId(String tenantId, String annotationTaskId) {
        return records.entrySet().stream()
                .filter(e -> e.getKey().startsWith(tenantId + ":"))
                .filter(e -> annotationTaskId.equals(e.getValue().annotationTaskId()))
                .map(Map.Entry::getValue)
                .sorted(Comparator.comparing(AnnotationSyncRecord::syncedAt).reversed())
                .toList();
    }

    @Override
    public List<AnnotationSyncRecord> findByNodeId(String tenantId, String nodeId) {
        return records.entrySet().stream()
                .filter(e -> e.getKey().startsWith(tenantId + ":"))
                .filter(e -> nodeId.equals(e.getValue().nodeId()))
                .map(Map.Entry::getValue)
                .sorted(Comparator.comparing(AnnotationSyncRecord::syncedAt).reversed())
                .toList();
    }

    @Override
    public List<AnnotationSyncRecord> findByStatus(String tenantId, AnnotationTaskStatus status) {
        return records.entrySet().stream()
                .filter(e -> e.getKey().startsWith(tenantId + ":"))
                .filter(e -> status == e.getValue().status())
                .map(Map.Entry::getValue)
                .sorted(Comparator.comparing(AnnotationSyncRecord::syncedAt).reversed())
                .toList();
    }

    @Override
    public PageResult<AnnotationSyncRecord> findPage(QuerySpec query) {
        var matching = records.entrySet().stream()
                .filter(e -> e.getKey().startsWith(query.tenantId() + ":"))
                .map(Map.Entry::getValue)
                .sorted(Comparator.comparing(AnnotationSyncRecord::syncedAt).reversed())
                .toList();
        int limit = query.page().pageSize();
        int offset = query.page().pageNumber() * limit;
        var items = offset >= matching.size() ? List.<AnnotationSyncRecord>of()
                : matching.subList(offset, Math.min(offset + limit, matching.size()));
        return new PageResult<>(items, query.page().pageNumber(), query.page().pageSize(), matching.size());
    }
}
